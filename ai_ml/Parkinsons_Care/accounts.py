from uuid import uuid4

from django.contrib.auth.models import User
from django.core.exceptions import ValidationError
from django.core.validators import validate_email
from django.db.models import Q
from rest_framework import status
from rest_framework.authtoken.models import Token
from rest_framework.authentication import TokenAuthentication
from rest_framework.exceptions import AuthenticationFailed
from rest_framework.decorators import api_view, authentication_classes, permission_classes
from rest_framework.permissions import AllowAny
from rest_framework.response import Response

from writing_test.models import Patient


def _token_for_user(user):
    return Token.objects.get_or_create(user=user)[0]


@api_view(["POST"])
@authentication_classes([])
@permission_classes([AllowAny])
def guest(request):
    username = f"guest_{uuid4().hex}"
    user = User.objects.create_user(username=username, password=None)
    Patient.objects.create(user=user)
    token = _token_for_user(user)
    return Response({"token": token.key, "is_guest": True}, status=status.HTTP_201_CREATED)


@api_view(["POST"])
@authentication_classes([])
@permission_classes([AllowAny])
def register(request):
    email = str(request.data.get("email", "")).strip().lower()
    password = request.data.get("password")
    if not email or not isinstance(password, str) or len(password) < 6:
        return Response(
            {"error": "A valid email and a password of at least 6 characters are required"},
            status=status.HTTP_400_BAD_REQUEST,
        )
    try:
        validate_email(email)
    except ValidationError:
        return Response({"error": "Enter a valid email address"}, status=status.HTTP_400_BAD_REQUEST)

    authentication = TokenAuthentication()
    authenticated = None
    if request.headers.get("Authorization"):
        try:
            authenticated = authentication.authenticate(request)
        except AuthenticationFailed:
            authenticated = None

    guest_user = authenticated[0] if authenticated and authenticated[0].username.startswith("guest_") else None
    existing = User.objects.filter(Q(username=email) | Q(email__iexact=email)).first()
    if existing and existing != guest_user:
        return Response({"error": "An account with this email already exists"}, status=status.HTTP_409_CONFLICT)

    if guest_user:
        guest_user.username = email
        guest_user.email = email
        guest_user.set_password(password)
        guest_user.save(update_fields=["username", "email", "password"])
        Patient.objects.get_or_create(user=guest_user)
        user = guest_user
    else:
        user = User.objects.create_user(username=email, email=email, password=password)
        Patient.objects.create(user=user)

    token = _token_for_user(user)
    return Response({"token": token.key, "is_guest": False}, status=status.HTTP_201_CREATED)

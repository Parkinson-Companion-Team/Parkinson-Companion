import logging
import os
import tempfile

from PIL import Image, UnidentifiedImageError
from rest_framework import status
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from ml_engine.write.predictor import analyze_drawing
from .models import DailyWritingTest, Patient

logger = logging.getLogger(__name__)

MAX_UPLOAD_BYTES = 10 * 1024 * 1024
ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png"}


class ParkinsonTestAnalysisView(APIView):
    parser_classes = [MultiPartParser, FormParser]
    permission_classes = [IsAuthenticated]

    def post(self, request, format=None):
        test_type = request.data.get("test_type")
        uploaded_file = request.data.get("uploaded_file")

        if test_type not in dict(DailyWritingTest.TEST_TYPE_CHOICES):
            return Response(
                {"error": "test_type must be 'spiral' or 'meander'"},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if not uploaded_file:
            return Response(
                {"error": "Missing uploaded_file"},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if uploaded_file.size > MAX_UPLOAD_BYTES:
            return Response(
                {"error": "Image exceeds the 10 MB limit"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        extension = os.path.splitext(uploaded_file.name)[1].lower()
        if extension not in ALLOWED_EXTENSIONS:
            return Response(
                {"error": "Only JPEG and PNG images are supported"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        temp_path = None
        try:
            with tempfile.NamedTemporaryFile(suffix=extension, delete=False) as tmp:
                for chunk in uploaded_file.chunks():
                    tmp.write(chunk)
                temp_path = tmp.name

            try:
                with Image.open(temp_path) as image:
                    image.verify()
            except (UnidentifiedImageError, OSError):
                return Response(
                    {"error": "Uploaded file is not a valid image"},
                    status=status.HTTP_400_BAD_REQUEST,
                )

            result = analyze_drawing(temp_path, test_type)
            patient, _ = Patient.objects.get_or_create(user=request.user)
            uploaded_file.seek(0)
            test_record = DailyWritingTest.objects.create(
                patient=patient,
                test_type=test_type,
                test_image=uploaded_file,
                stability_score=result["stability_score"],
                distance=result["distance"],
                embedding=result["embedding"],
                model_version=result["model_version"],
            )
            return Response(
                {
                    "success": True,
                    "data": {
                        "id": test_record.id,
                        "test_type": test_record.test_type,
                        "stability_score": test_record.stability_score,
                        "distance": test_record.distance,
                        "created_at": test_record.created_at.isoformat(),
                        "interpretation": "Trend indicator for monitoring, not a diagnosis.",
                    },
                },
                status=status.HTTP_201_CREATED,
            )
        except Exception:
            logger.exception("Writing test processing failed for user %s", request.user.pk)
            return Response(
                {"error": "Unable to process the handwriting sample."},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )
        finally:
            if temp_path and os.path.exists(temp_path):
                os.remove(temp_path)

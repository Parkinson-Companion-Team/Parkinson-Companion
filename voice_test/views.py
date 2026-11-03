import logging
import os
import tempfile

from rest_framework import status
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from soundfile import info as audio_info

from ml_engine.voice.anchor import get_or_create_baseline, get_recent_mean_embedding
from ml_engine.voice.inference import run_voice_inference
from voice_test.models import VoiceTestResult
from writing_test.models import Patient

logger = logging.getLogger(__name__)


class VoiceTestUploadView(APIView):
    parser_classes = [MultiPartParser, FormParser]
    permission_classes = [IsAuthenticated]

    def post(self, request, format=None):
        audio_file = request.data.get("audio")
        task_type = request.data.get("task_type")

        if not audio_file:
            return Response({"error": "No audio file provided"}, status=status.HTTP_400_BAD_REQUEST)
        if task_type not in dict(VoiceTestResult.TASK_CHOICES):
            return Response({"error": "Invalid or missing task_type"}, status=status.HTTP_400_BAD_REQUEST)

        patient, _ = Patient.objects.get_or_create(user=request.user)
        temp_path = None
        try:
            with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
                for chunk in audio_file.chunks():
                    tmp.write(chunk)
                temp_path = tmp.name

            baseline_embedding = get_or_create_baseline(patient, task_type, temp_path)
            recent_mean_embedding = get_recent_mean_embedding(patient, task_type, days=30)
            result = run_voice_inference(
                audio_path=temp_path,
                baseline_embedding=baseline_embedding,
                recent_mean_embedding=recent_mean_embedding,
            )

            test_result = VoiceTestResult.objects.create(
                patient=patient,
                task_type=task_type,
                stability_score=result["stability_vs_baseline"],
                stability_vs_recent=result.get("stability_vs_recent"),
                embedding=result["embedding"],
                raw_features=result["raw_features"],
                model_version=result["model_version"],
                audio_duration_sec=audio_info(temp_path).duration,
            )
            response_data = {
                "id": test_result.id,
                "stability_score": result["stability_vs_baseline"],
                "raw_features": result["raw_features"],
                "created_at": test_result.created_at.isoformat(),
                "interpretation": "Trend indicator for monitoring, not a diagnosis.",
            }
            if "stability_vs_recent" in result:
                response_data["stability_vs_recent"] = result["stability_vs_recent"]
            return Response(response_data, status=status.HTTP_201_CREATED)
        except Exception:
            logger.exception("Voice test processing failed for user %s", request.user.pk)
            return Response(
                {"error": "Unable to process the audio recording."},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )
        finally:
            if temp_path and os.path.exists(temp_path):
                os.remove(temp_path)


upload_voice_test = VoiceTestUploadView.as_view()

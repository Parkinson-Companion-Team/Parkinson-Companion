from django.urls import path
from writing_test.views import ParkinsonTestAnalysisView

urlpatterns = [
    path("analyze/", ParkinsonTestAnalysisView.as_view(), name="analyze_writing_test"),
]

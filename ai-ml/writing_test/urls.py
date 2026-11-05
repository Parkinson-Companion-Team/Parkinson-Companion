from django.urls import path
from writing_test.views import ParkinsonTestAnalysisView, WritingHistoryView

urlpatterns = [
    path("analyze/", ParkinsonTestAnalysisView.as_view(), name="analyze_writing_test"),
    path("history/", WritingHistoryView.as_view(), name="writing_history"),
]

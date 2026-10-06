from django.urls import path

from trips.views import DailySummaryView, TripListView

urlpatterns = [
    path("trips/summary/", DailySummaryView.as_view(), name="trip-summary"),
    path("trips/", TripListView.as_view(), name="trip-list"),
]

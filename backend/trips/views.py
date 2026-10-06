from datetime import date

from rest_framework import status
from rest_framework.generics import ListCreateAPIView
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from trips.serializers import DateQuerySerializer, DailySummarySerializer, TripSerializer
from trips.services import create_trip, get_daily_summary, get_trips_for_date


def parse_query_date(query_params) -> date:
    serializer = DateQuerySerializer(data=query_params)
    serializer.is_valid(raise_exception=True)
    return serializer.validated_data["date"]


class TripListView(ListCreateAPIView):
    serializer_class = TripSerializer
    pagination_class = None
    http_method_names = ["get", "post"]

    def get_queryset(self):
        day = parse_query_date(self.request.query_params)
        return get_trips_for_date(day)

    def create(self, request: Request, *args, **kwargs) -> Response:
        trip = create_trip(request.data)
        serializer = self.get_serializer(trip)
        return Response(serializer.data, status=status.HTTP_201_CREATED)


class DailySummaryView(APIView):
    http_method_names = ["get"]

    def get(self, request: Request) -> Response:
        day = parse_query_date(request.query_params)
        summary = get_daily_summary(day)
        payload = DailySummarySerializer({"date": day, **summary})
        return Response(payload.data)

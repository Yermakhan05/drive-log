from datetime import date, datetime, time, timedelta
from decimal import Decimal
from typing import Any, Mapping, TypedDict

from django.db import IntegrityError, transaction
from django.db.models import Count, Q, QuerySet, Sum, Value
from django.db.models.functions import Coalesce
from django.utils import timezone

from trips.models import Trip
from trips.serializers import TripSerializer

ZERO = Decimal("0")


class DuplicateTripError(Exception):
    def __init__(self, trip_id: str) -> None:
        self.trip_id = trip_id
        super().__init__(f"A trip with id '{trip_id}' already exists.")


def is_duplicate_id_error(exc: IntegrityError) -> bool:
    message = str(exc).lower()
    return "unique" in message or "primary key" in message


def create_trip(data: Mapping[str, Any]) -> Trip:
    serializer = TripSerializer(data=data)
    serializer.is_valid(raise_exception=True)
    trip_id = serializer.validated_data["id"]
    try:
        with transaction.atomic():
            return serializer.save()
    except IntegrityError as exc:
        if is_duplicate_id_error(exc):
            raise DuplicateTripError(trip_id) from exc
        raise


class DailySummary(TypedDict):
    trips_count: int
    revenue: Decimal
    commission: Decimal
    net_income: Decimal
    cash: Decimal
    card: Decimal


def calendar_day_bounds(day: date) -> tuple[datetime, datetime]:
    """Return [start, end) datetime bounds for a calendar day in the current timezone."""
    tz = timezone.get_current_timezone()
    day_start = timezone.make_aware(datetime.combine(day, time.min), tz)
    day_end = day_start + timedelta(days=1)
    return day_start, day_end


def get_trips_for_date(day: date) -> QuerySet[Trip]:
    day_start, day_end = calendar_day_bounds(day)
    return Trip.objects.filter(start__gte=day_start, start__lt=day_end).order_by("start")


def get_daily_summary(day: date) -> DailySummary:
    aggregates = get_trips_for_date(day).aggregate(
        trips_count=Count("id"),
        revenue=Coalesce(Sum("amount"), Value(ZERO)),
        commission=Coalesce(Sum("commission"), Value(ZERO)),
        cash=Coalesce(
            Sum("amount", filter=Q(payment=Trip.PAYMENT_CASH)),
            Value(ZERO),
        ),
        card=Coalesce(
            Sum("amount", filter=Q(payment=Trip.PAYMENT_CARD)),
            Value(ZERO),
        ),
    )
    revenue = Decimal(aggregates["revenue"])
    commission = Decimal(aggregates["commission"])
    return DailySummary(
        trips_count=aggregates["trips_count"],
        revenue=revenue,
        commission=commission,
        net_income=revenue - commission,
        cash=Decimal(aggregates["cash"]),
        card=Decimal(aggregates["card"]),
    )

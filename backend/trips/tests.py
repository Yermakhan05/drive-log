from datetime import date
from decimal import Decimal

from django.test import TestCase
from django.utils.dateparse import parse_datetime
from rest_framework import status
from rest_framework.test import APIClient

from trips.models import Trip
from trips.services import get_daily_summary


DAY = date(2026, 10, 1)

CARD_TRIP = {
    "id": "trip-card",
    "start": "2026-10-01T12:00:00Z",
    "end": "2026-10-01T12:40:00Z",
    "amount": "2400.00",
    "payment": "card",
    "commission": "360.00",
}

CASH_TRIP = {
    "id": "trip-cash",
    "start": "2026-10-01T08:00:00Z",
    "end": "2026-10-01T08:30:00Z",
    "amount": "1500.00",
    "payment": "cash",
    "commission": "225.00",
}


def create_trip_record(**overrides) -> Trip:
    payload = {
        "id": "trip-default",
        "start": parse_datetime("2026-10-01T08:00:00Z"),
        "end": parse_datetime("2026-10-01T09:00:00Z"),
        "amount": Decimal("100.00"),
        "payment": Trip.PAYMENT_CASH,
        "commission": Decimal("10.00"),
        **overrides,
    }
    return Trip.objects.create(**payload)


class TestDailySummary(TestCase):
    def setUp(self) -> None:
        create_trip_record(
            id="trip-card",
            start=parse_datetime("2026-10-01T12:00:00Z"),
            end=parse_datetime("2026-10-01T12:40:00Z"),
            amount=Decimal("2400"),
            payment=Trip.PAYMENT_CARD,
            commission=Decimal("360"),
        )
        create_trip_record(
            id="trip-cash",
            start=parse_datetime("2026-10-01T08:00:00Z"),
            end=parse_datetime("2026-10-01T08:30:00Z"),
            amount=Decimal("1500"),
            payment=Trip.PAYMENT_CASH,
            commission=Decimal("225"),
        )

    def test_daily_summary_totals(self) -> None:
        summary = get_daily_summary(DAY)
        self.assertEqual(summary["trips_count"], 2)
        self.assertEqual(summary["revenue"], Decimal("3900"))
        self.assertEqual(summary["commission"], Decimal("585"))
        self.assertEqual(summary["net_income"], Decimal("3315"))
        self.assertEqual(summary["cash"], Decimal("1500"))
        self.assertEqual(summary["card"], Decimal("2400"))

    def test_trip_from_another_day_is_excluded(self) -> None:
        create_trip_record(
            id="trip-next-day",
            start=parse_datetime("2026-10-02T00:00:00Z"),
            end=parse_datetime("2026-10-02T00:30:00Z"),
            amount=Decimal("999"),
            payment=Trip.PAYMENT_CASH,
            commission=Decimal("99"),
        )
        summary = get_daily_summary(DAY)
        self.assertEqual(summary["trips_count"], 2)
        self.assertEqual(summary["revenue"], Decimal("3900"))
        self.assertEqual(summary["commission"], Decimal("585"))
        self.assertEqual(summary["net_income"], Decimal("3315"))
        self.assertEqual(summary["cash"], Decimal("1500"))
        self.assertEqual(summary["card"], Decimal("2400"))


class TestDuplicateTrip(TestCase):
    def setUp(self) -> None:
        self.client = APIClient()

    def test_second_post_with_same_id_returns_conflict(self) -> None:
        first = self.client.post("/api/trips/", CARD_TRIP, format="json")
        second = self.client.post("/api/trips/", CARD_TRIP, format="json")

        self.assertEqual(first.status_code, status.HTTP_201_CREATED)
        self.assertEqual(second.status_code, status.HTTP_409_CONFLICT)
        self.assertEqual(Trip.objects.filter(id=CARD_TRIP["id"]).count(), 1)
        self.assertEqual(Trip.objects.count(), 1)
        self.assertIn("id", second.json()["errors"])


class TestCreateTripValidation(TestCase):
    def setUp(self) -> None:
        self.client = APIClient()

    def test_amount_must_be_greater_than_zero(self) -> None:
        payload = {**CASH_TRIP, "amount": "0"}
        response = self.client.post("/api/trips/", payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("amount", response.json()["errors"])
        self.assertEqual(Trip.objects.count(), 0)

    def test_end_must_be_later_than_start(self) -> None:
        payload = {
            **CASH_TRIP,
            "start": "2026-10-01T10:00:00Z",
            "end": "2026-10-01T09:00:00Z",
        }
        response = self.client.post("/api/trips/", payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("end", response.json()["errors"])
        self.assertEqual(Trip.objects.count(), 0)

    def test_payment_must_be_cash_or_card(self) -> None:
        payload = {**CASH_TRIP, "payment": "crypto"}
        response = self.client.post("/api/trips/", payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("payment", response.json()["errors"])
        self.assertEqual(Trip.objects.count(), 0)

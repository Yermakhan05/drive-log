# Driver Shift Diary

Driver Shift Diary is a small full-stack project for tracking driver trips by day.
It contains:

- `backend/` - Django REST Framework API.
- `DriveLog/` - Android app written in Kotlin with XML layouts, Retrofit, ViewModel, and StateFlow.

## What Is Implemented

### Backend

The backend exposes trip APIs under `/api/`:

- `GET /api/trips/?date=YYYY-MM-DD` - returns trips for the selected day.
- `GET /api/trips/summary/?date=YYYY-MM-DD` - returns a daily summary.
- `POST /api/trips/` - creates a trip.

Trip fields:

- `id`
- `start`
- `end`
- `amount`
- `payment`
- `commission`

Implemented validation:

- `amount > 0`
- `commission >= 0`
- `end > start`
- `payment` must be `cash` or `card`
- duplicate trip IDs return HTTP `409`
- trip ID is the database primary key, so duplicates are protected by a database constraint

Daily summary includes:

- trip count
- revenue
- commission
- net income
- cash total
- card total

Backend tests cover:

- daily summary totals
- excluding trips from another day
- duplicate trip protection
- create validation for amount, end time, and payment

### Android

The Android app implements:

- Driver Diary main screen.
- selected date display.
- previous/next day buttons.
- automatic reload when the selected date changes.
- loading, success, and error states.
- daily summary display.
- trip list display.
- Add Trip screen.
- payment picker for Cash/Card.
- date/time pickers for start and end.
- client-side validation before POST.
- user-friendly API error messages.
- special duplicate ID message: `Trip with this ID already exists.`

The Android project follows this simple structure:

```text
data/
  api/
  model/
  repository/

ui/
  screen/
  components/

viewmodel/
```

## Run Backend

From the repository root:

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python manage.py migrate
python manage.py runserver 0.0.0.0:8000
```

Optional seed import:

```bash
python manage.py import_trips seed_data.json
```

Run backend tests:

```bash
cd backend
source .venv/bin/activate
python manage.py test
```

## Run Android

Open `DriveLog/` in Android Studio.

The app uses:

```text
http://10.0.2.2:8000/
```

as `BASE_URL`, which points from the Android emulator to the host machine.

Make sure the backend is running before opening the app.

Build from terminal:

```bash
cd DriveLog
./gradlew assembleDebug
```

## Notes

- The backend currently uses SQLite for local development.
- Android cleartext HTTP is allowed only for local development hosts: `10.0.2.2`, `localhost`, and `127.0.0.1`.
- This project is configured for development, not production deployment.

# Mobile Driver Tracking Flow

This backend now supports a simple tracking flow for the driver app.

## 1. Login

Request:

```http
POST /api/mobile/auth/login
Content-Type: application/json

{
  "email": "driver@example.com",
  "password": "secret"
}
```

Use the returned `token` as:

```http
Authorization: Bearer <token>
```

## 2. Ask for current active trip

Request:

```http
GET /api/driver/trips/active
Authorization: Bearer <token>
```

Response when the driver has an active trip:

```json
{
  "hasActiveTrip": true,
  "tripType": "SOBER",
  "tripId": 15,
  "status": "ACCEPTED",
  "pickupAddress": "123 Nguyen Hue, Quan 1",
  "shouldTrackLocation": true,
  "canStartTrip": false,
  "canCompleteTrip": false
}
```

Response when there is no active trip:

```json
{
  "hasActiveTrip": false,
  "tripType": null,
  "tripId": null,
  "status": null,
  "pickupAddress": null,
  "shouldTrackLocation": false,
  "canStartTrip": false,
  "canCompleteTrip": false
}
```

## 3. Start foreground location tracking in the app

Recommended app behavior:

- After login, call `/api/driver/trips/active`.
- If `shouldTrackLocation=true`, start a foreground service.
- Request location permission and post GPS every 3 to 5 seconds.
- Re-check `/api/driver/trips/active` when the app returns to foreground.
- Stop the foreground service when `hasActiveTrip=false`.

## 4. Send location updates

Request:

```http
POST /api/driver/locations
Authorization: Bearer <token>
Content-Type: application/json

{
  "tripType": "SOBER",
  "tripId": 15,
  "lat": 10.7769,
  "lng": 106.7009,
  "speed": 8.5,
  "bearing": 120.0,
  "accuracy": 12.0,
  "recordedAt": "2026-03-21T14:05:00Z"
}
```

When this succeeds:

- The backend stores the latest location.
- The backend pushes the new location to `/topic/trips/{tripType}/{tripId}`.
- The customer tracking page updates the marker in realtime.

## 5. Trip actions

Sober booking:

- `POST /api/driver/sober-bookings/{id}/accept`
- `POST /api/driver/sober-bookings/{id}/arrive`
- `POST /api/driver/sober-bookings/{id}/start`
- `POST /api/driver/sober-bookings/{id}/complete`

Rental:

- `POST /api/driver/rentals/{id}/accept`
- `POST /api/driver/rentals/{id}/start`
- `POST /api/driver/rentals/{id}/complete`

## Suggested app rule

Use this simple rule in the app:

- If the driver has accepted or is already running a trip, start location tracking.
- Do not wait until the trip is fully started.
- This lets the customer see the driver moving toward the pickup point too.

# Mobile app

## Connecting to the backend

The Android emulator uses `http://10.0.2.2:8000` to reach a backend running on
the development laptop. For a real phone, change the base URL in
`ApiClient.java` to the laptop's LAN IP address, keep the phone and laptop on
the same Wi-Fi network, and run Django with:

```powershell
python manage.py runserver 0.0.0.0:8000
```

# Parkinsons_Care

## Setup

Run the following commands from inside `ai_ml`:

Create and activate a virtual environment:

```powershell
cd ai_ml
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu
pip install -r requirements.txt
python manage.py migrate
```

The dataset is git-ignored and uses:

```text
ml_engine/data/<spiral|meander>/<healthy*|patient*>/<name>-<ID>.jpg
```

## Models

Train, evaluate, and calibrate each drawing type separately:

```powershell
python -m ml_engine.write.train --type spiral
python -m ml_engine.write.evaluate --type spiral
python -m ml_engine.write.calibrate --type spiral
```

Replace `spiral` with `meander` as needed. Run the server with:

```powershell
python manage.py runserver
```

## API for the mobile app

- `POST /guest/` creates a guest account and returns a token.
- `POST /register/` accepts `email` and `password` and returns a token.
- `POST /api-token-auth/` accepts form fields `username=email` and `password`.
- `POST /writing-test/analyze/` accepts multipart `test_type=spiral|meander` and
  `uploaded_file=PNG/JPG`.
- `GET /writing-test/history/` returns the authenticated user's writing history.
- `POST /voice-test/upload/` accepts multipart `task_type=vowel|ddk|reading` and
  `audio=WAV 16 kHz mono PCM16`.

Create a token through Django or the token endpoint, then send it with requests:

```powershell
curl.exe -X POST http://127.0.0.1:8000/api-token-auth/ -d "username=user&password=password"
curl.exe -X POST http://127.0.0.1:8000/writing-test/analyze/ `
  -H "Authorization: Token <TOKEN>" `
  -F "test_type=spiral" -F "uploaded_file=@drawing.png"
curl.exe "http://127.0.0.1:8000/writing-test/history/?test_type=spiral&days=90" `
  -H "Authorization: Token <TOKEN>"
```

Images should have an opaque background, dark strokes on light, a square canvas of at least 800 pixels, JPG or PNG format, and be no larger than 10 MB.

## Known limitations

- The dataset is scanned paper; touchscreen drawings are untested.
- Validation contains only 5 PD patient groups.
- Healthy participants skew younger than PD participants, creating an age confound.
- Byte-identical drawings were filed under different patient IDs; `build_patient_groups` merges them.

# Parkinson Companion

A mobile companion app and AI/ML API for Parkinson's care and assessment.

Final-year group project, Brainware University, Year 4

## Team

| Module | Folder | Contributor |
|---|---|---|
| Mobile app (frontend) | /mobile-app | Susmita Biswas (@Suhasini100) |
| AI/ML models & API | /ai-ml | Jotirmoy Mondal (@Jotirmoy-Mondal) |

## How the two parts connect

The mobile app is the client for the Django API in `/ai-ml`. The intended flow is for
the app to upload a drawing or voice sample to the API's authenticated analysis
endpoint, such as `/writing-test/analyze/`, and display the returned assessment.
The current frontend does not yet contain an ML API client, so the endpoint and
authentication wiring still need to be connected.

## Mobile app setup

Open `/mobile-app` in Android Studio, allow Gradle to sync, and run the `app`
configuration on an emulator or Android device. The Android module is located at
`/mobile-app/app`.

## AI/ML setup

From `/ai-ml`, create and activate a virtual environment and install the
dependencies:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu
pip install -r requirements.txt
python manage.py migrate
python manage.py runserver
```

Train, evaluate, and calibrate the writing models separately:

```powershell
python -m ml_engine.write.train --type spiral
python -m ml_engine.write.evaluate --type spiral
python -m ml_engine.write.calibrate --type spiral
```

Replace `spiral` with `meander` as needed. The dataset belongs in
`/ai-ml/ml_engine/data/` and is intentionally ignored by Git. The API accepts
authenticated requests at `/api-token-auth/`, `/writing-test/analyze/`, and
`/writing-test/history/`.

Known limitations include scanned-paper training data, untested touchscreen
drawings, only five Parkinson's patient groups in validation, an age confound
between healthy and Parkinson's participants, and duplicate drawings filed
under different patient IDs.

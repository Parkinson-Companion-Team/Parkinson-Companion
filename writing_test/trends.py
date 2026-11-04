import numpy as np

BASELINE_SESSIONS = 3
EWMA_ALPHA = 0.3


def ewma(values, alpha=EWMA_ALPHA):
    """None values are skipped; output keeps the last smoothed value (None before the first real value)."""
    out, prev = [], None
    for v in values:
        if v is not None:
            prev = v if prev is None else alpha * v + (1 - alpha) * prev
        out.append(prev)
    return out


def mean_embedding(embeddings):
    return np.mean(np.asarray(embeddings, dtype=float), axis=0).tolist()


def euclidean(a, b):
    return float(np.linalg.norm(np.asarray(a, dtype=float) - np.asarray(b, dtype=float)))


def build_history(rows, baseline_sessions=BASELINE_SESSIONS, alpha=EWMA_ALPHA, since=None):
    baseline_rows = [row for row in rows if row.get("embedding") is not None][:baseline_sessions]
    baseline_ready = len(baseline_rows) >= baseline_sessions
    baseline = mean_embedding([row["embedding"] for row in baseline_rows]) if baseline_rows else None
    baseline_noise = (
        float(np.mean([euclidean(row["embedding"], baseline) for row in baseline_rows]))
        if baseline_ready else None
    )
    points = []
    for row in rows:
        personal_distance = euclidean(row["embedding"], baseline) if baseline_ready and row.get("embedding") else None
        point = {
            "id": row["id"],
            "created_at": row["created_at"],
            "stability_score": row["stability_score"],
            "distance": row["distance"],
            "personal_distance": personal_distance,
            "is_baseline": row in baseline_rows,
        }
        points.append(point)
    stability = ewma([point["stability_score"] for point in points], alpha)
    personal = ewma([point["personal_distance"] for point in points], alpha)
    for point, stable, personal_distance in zip(points, stability, personal):
        point["smoothed_stability"] = stable
        point["smoothed_personal_distance"] = personal_distance
    if since is not None:
        points = [point for point in points if point["created_at"] >= since]
    weekly_map = {}
    for point in points:
        iso = point["created_at"].isocalendar()
        week = f"{iso.year}-W{iso.week:02d}"
        bucket = weekly_map.setdefault(week, [])
        bucket.append(point)
    weekly = [
        {
            "week": week,
            "sessions": len(bucket),
            "mean_stability": float(np.mean([p["stability_score"] for p in bucket if p["stability_score"] is not None]))
            if any(p["stability_score"] is not None for p in bucket) else None,
            "mean_personal_distance": float(np.mean([p["personal_distance"] for p in bucket if p["personal_distance"] is not None]))
            if any(p["personal_distance"] is not None for p in bucket) else None,
        }
        for week, bucket in weekly_map.items()
    ]
    return {
        "baseline_ready": baseline_ready,
        "baseline_sessions": baseline_sessions,
        "baseline_noise": baseline_noise,
        "points": points,
        "weekly": weekly,
    }



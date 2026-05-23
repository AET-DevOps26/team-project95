FROM python:3.11-slim

WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY services/genai-service/pyproject.toml /app/pyproject.toml
COPY services/genai-service/app.py /app/app.py
COPY services/genai-service/README.md /app/README.md

RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir .

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8000

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]

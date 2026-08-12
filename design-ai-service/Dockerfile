FROM python:3.12-slim

WORKDIR /app

RUN useradd \
    --create-home \
    --uid 10001 \
    aiworker

COPY requirements.txt ./

RUN pip install \
    --no-cache-dir \
    --requirement requirements.txt

COPY app ./app

USER aiworker

CMD ["python", "-m", "app.main"]
#!/usr/bin/env bash
set -euo pipefail

required_variables=(
  ANDROID_HOME
  RELEASE_STORE_FILE
  RELEASE_STORE_PASSWORD
  RELEASE_KEY_ALIAS
  RELEASE_KEY_PASSWORD
  ROTATED_RELEASE_STORE_FILE
  ROTATED_RELEASE_STORE_PASSWORD
  ROTATED_RELEASE_KEY_ALIAS
  ROTATED_RELEASE_KEY_PASSWORD
  SIGNING_LINEAGE_FILE
  LEGACY_RELEASE_CERT_SHA256
  ROTATED_RELEASE_CERT_SHA256
)

for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    echo "필수 서명 환경변수가 없습니다: ${variable_name}" >&2
    exit 1
  fi
done

if [[ "$#" -eq 0 ]]; then
  echo "서명할 APK 경로를 하나 이상 지정해야 합니다." >&2
  exit 1
fi

apksigner_path="$(find "$ANDROID_HOME/build-tools" -type f \( -name apksigner -o -name apksigner.bat \) | sort -V | tail -n 1)"
if [[ ! -f "$apksigner_path" ]]; then
  echo "실행 가능한 apksigner를 찾지 못했습니다." >&2
  exit 1
fi

normalize_digest() {
  printf '%s' "$1" | tr -d ':' | tr '[:upper:]' '[:lower:]'
}

legacy_certificate="$(normalize_digest "$LEGACY_RELEASE_CERT_SHA256")"
rotated_certificate="$(normalize_digest "$ROTATED_RELEASE_CERT_SHA256")"

keystore_digest() {
  local keystore_path="$1"
  local key_alias="$2"
  local store_password="$3"
  keytool -exportcert \
    -keystore "$keystore_path" \
    -alias "$key_alias" \
    -storepass "$store_password" \
    2>/dev/null \
    | sha256sum \
    | awk '{print $1}'
}

actual_legacy_certificate="$(keystore_digest "$RELEASE_STORE_FILE" "$RELEASE_KEY_ALIAS" "$RELEASE_STORE_PASSWORD")"
actual_rotated_certificate="$(keystore_digest "$ROTATED_RELEASE_STORE_FILE" "$ROTATED_RELEASE_KEY_ALIAS" "$ROTATED_RELEASE_STORE_PASSWORD")"

if [[ "$actual_legacy_certificate" != "$legacy_certificate" ]]; then
  echo "기존 키스토어 인증서가 승인된 지문과 다릅니다." >&2
  exit 1
fi
if [[ "$actual_rotated_certificate" != "$rotated_certificate" ]]; then
  echo "새 키스토어 인증서가 승인된 지문과 다릅니다." >&2
  exit 1
fi

for apk_path in "$@"; do
  if [[ ! -f "$apk_path" ]]; then
    echo "APK 파일을 찾지 못했습니다: ${apk_path}" >&2
    exit 1
  fi

  "$apksigner_path" sign \
    --ks "$RELEASE_STORE_FILE" \
    --ks-key-alias "$RELEASE_KEY_ALIAS" \
    --ks-pass env:RELEASE_STORE_PASSWORD \
    --key-pass env:RELEASE_KEY_PASSWORD \
    --next-signer \
    --ks "$ROTATED_RELEASE_STORE_FILE" \
    --ks-key-alias "$ROTATED_RELEASE_KEY_ALIAS" \
    --ks-pass env:ROTATED_RELEASE_STORE_PASSWORD \
    --key-pass env:ROTATED_RELEASE_KEY_PASSWORD \
    --lineage "$SIGNING_LINEAGE_FILE" \
    --rotation-min-sdk-version 28 \
    "$apk_path"

  "$apksigner_path" verify --verbose --min-sdk-version 26 "$apk_path"
  certificate_output="$("$apksigner_path" verify --print-certs "$apk_path")"
  normalized_output="$(normalize_digest "$certificate_output")"

  if ! grep -Fq "$rotated_certificate" <<<"$normalized_output"; then
    echo "APK에서 새 인증서를 확인하지 못했습니다: ${apk_path}" >&2
    exit 1
  fi

  echo "서명 계보 검증 완료: ${apk_path}"
done

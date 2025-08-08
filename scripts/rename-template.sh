#!/usr/bin/env bash
set -euo pipefail

# Template rename tool for this repository
# - Reads config from scripts/rename.config.env
# - Applies content replacements first
# - Renames directories and Android package source folders
#
# Safe on macOS and Linux. Requires: bash, perl, find, xargs

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)

CONFIG_FILE="${1:-$SCRIPT_DIR/rename.config.env}"
if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Config file not found: $CONFIG_FILE" >&2
  echo "Please copy $SCRIPT_DIR/rename.config.default.env to $CONFIG_FILE and edit it." >&2
  exit 1
fi

# shellcheck disable=SC1090
source "$CONFIG_FILE"

confirm() {
  local prompt="$1"
  if [[ "${CONFIRM:-no}" == "yes" ]]; then
    return 0
  fi
  read -r -p "$prompt [y/N]: " ans
  if [[ "$ans" != "y" && "$ans" != "Y" ]]; then
    echo "Cancelled."
    exit 1
  fi
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_cmd perl
require_cmd find
require_cmd xargs

# Build file list excluding binary/derived dirs
build_file_list() {
  # Exclude heavy/binary directories and this script/config to avoid self-editing
  # - iOS prebuilt frameworks inside uni_modules
  # - Any *.framework bundles inside iOS projects
  # - Gradle/wrapper/cache/build outputs
  # - Node modules if present
  # - .git and IDE folders
  find "$ROOT_DIR" \
    \( \
      -path "$SCRIPT_DIR/rename.config.env" -o \
      -path "$SCRIPT_DIR/rename-template.sh" -o \
      -path "*/.git/*" -o \
      -path "*/.gradle/*" -o \
      -path "*/build/*" -o \
      -path "*/node_modules/*" -o \
      -path "*/.idea/*" -o \
      -path "*/.vscode/*" -o \
      -path "*/uniapp-*/uni_modules/*/utssdk/app-ios/Frameworks/*" -o \
      -path "*/ios-*/**/*.framework/*" -o \
      -path "*/ios-*/**/*.framework" -o \
      -name "*.jar" -o \
      -name "*.aar" -o \
      -name "*.png" -o \
      -name "*.jpg" -o \
      -name "*.jpeg" -o \
      -name "*.webp" -o \
      -name "*.gif" -o \
      -name "*.ttf" -o \
      -name "*.otf" -o \
      -name "*.ico" -o \
      -name "*.pdf" \
    \) -prune -o -type f -print0
}

replace_literal_in_file() {
  local file="$1" from="$2" to="$3"
  FROM="$from" TO="$to" perl -0777 -i -pe 'BEGIN { $from=$ENV{FROM}; $to=$ENV{TO}; } s/\Q$from\E/$to/g' "$file"
}

do_replacements() {
  local -a pairs

  add_pair_if_set() {
    local from_val="$1" to_val="$2"
    if [[ -n "$to_val" && "$to_val" != "$from_val" ]]; then
      pairs+=("$from_val" "$to_val")
    fi
  }

  # Android packages
  add_pair_if_set "${ANDROID_LIBRARY_PACKAGE_FROM}" "${ANDROID_LIBRARY_PACKAGE_TO}"
  add_pair_if_set "${ANDROID_APP_PACKAGE_FROM}" "${ANDROID_APP_PACKAGE_TO}"

  # Android module dir names and root dir names referenced in scripts/configs
  add_pair_if_set "${ANDROID_LIBRARY_MODULE_DIR_FROM}" "${ANDROID_LIBRARY_MODULE_DIR_TO}"
  # DO NOT globally replace app module name ("app") to avoid false positives
  add_pair_if_set "${ANDROID_ROOT_DIR_FROM}" "${ANDROID_ROOT_DIR_TO}"

  # iOS framework names and bundle ids
  add_pair_if_set \"${IOS_FRAMEWORK_PROJECT_NAME_FROM}\" \"${IOS_FRAMEWORK_PROJECT_NAME_TO}\"
  add_pair_if_set \"${IOS_FRAMEWORK_PRODUCT_NAME_FROM}\" \"${IOS_FRAMEWORK_PRODUCT_NAME_TO}\"
  add_pair_if_set \"${IOS_FRAMEWORK_BUNDLE_ID_FROM}\" \"${IOS_FRAMEWORK_BUNDLE_ID_TO}\"
  
  # iOS framework name in build script\n  add_pair_if_set \"FRAMEWORK_NAME=\\\"${IOS_FRAMEWORK_PRODUCT_NAME_FROM}\\\"\" \"FRAMEWORK_NAME=\\\"${IOS_FRAMEWORK_PRODUCT_NAME_TO}\\\"\"

  # iOS app project and bundle id
  add_pair_if_set \"${IOS_APP_PROJECT_NAME_FROM}\" \"${IOS_APP_PROJECT_NAME_TO}\"
  add_pair_if_set \"${IOS_APP_BUNDLE_ID_FROM}\" \"${IOS_APP_BUNDLE_ID_TO}\"
  
  # iOS directory paths in build scripts
  add_pair_if_set \"../${IOS_APP_DIR_FROM}\" \"../${IOS_APP_DIR_TO}\"
  
  # iOS playground directory in build script
  add_pair_if_set \"../${IOS_APP_DIR_FROM}/${IOS_APP_PROJECT_NAME_FROM}\" \"../${IOS_APP_DIR_TO}/${IOS_APP_PROJECT_NAME_TO}\"

  # UniApp dir and UTS module info
  add_pair_if_set "${UNIAPP_DIR_FROM}" "${UNIAPP_DIR_TO}"
  add_pair_if_set "uni_modules/${UTS_MODULE_ID_FROM}" "uni_modules/${UTS_MODULE_ID_TO}"
  add_pair_if_set "${UTS_MODULE_ID_FROM}" "${UTS_MODULE_ID_TO}"
  add_pair_if_set "${UTS_MODULE_DISPLAY_NAME_FROM}" "${UTS_MODULE_DISPLAY_NAME_TO}"
  add_pair_if_set "${UTS_MODULE_DESCRIPTION_FROM}" "${UTS_MODULE_DESCRIPTION_TO}"
  
  # Additional iOS framework replacements
  # iOS framework name in Xcode project file
  add_pair_if_set "path = ${IOS_FRAMEWORK_PRODUCT_NAME_FROM}.framework" "path = ${IOS_FRAMEWORK_PRODUCT_NAME_TO}.framework"
  add_pair_if_set "${IOS_FRAMEWORK_PRODUCT_NAME_FROM}.framework" "${IOS_FRAMEWORK_PRODUCT_NAME_TO}.framework"

  if (( ${#pairs[@]} == 0 )); then
    echo "No content replacements to apply."
    return 0
  fi

  echo "Applying content replacements..."
  # Apply each pair across all files (stream to avoid bash 3.2 mapfile)
  local i from to
  for ((i=0; i<${#pairs[@]}; i+=2)); do
    from="${pairs[i]}"; to="${pairs[i+1]}"
    echo "- Replace: '$from' -> '$to'"
    build_file_list | while IFS= read -r -d $'\0' file; do
      replace_literal_in_file "$file" "$from" "$to"
    done
  done
}

# Move a package directory like com.example.app under given module's src roots
move_android_package_dirs() {
  local module_root="$1" old_pkg="$2" new_pkg="$3"
  [[ -z "$new_pkg" || "$new_pkg" == "$old_pkg" ]] && return 0

  local src_roots=("src/main/java" "src/androidTest/java" "src/test/java")

  local old_path new_path
  old_path="${old_pkg//./\/}"
  new_path="${new_pkg//./\/}"

  for sr in "${src_roots[@]}"; do
    local base="$module_root/$sr"
    if [[ -d "$base/$old_path" ]]; then
      mkdir -p "$base/$(dirname "$new_path")"
      echo "Moving package dir: $base/$old_path -> $base/$new_path"
      mv "$base/$old_path" "$base/$new_path"
      # Clean up any now-empty parent directories under base
      find "$base" -type d -empty -delete || true
    fi
  done
}

rename_dir_if_needed() {
  local from_abs="$1" to_abs="$2"
  if [[ "$from_abs" == "$to_abs" ]]; then
    return 0
  fi
  if [[ -e "$to_abs" ]]; then
    echo "Cannot rename: destination exists -> $to_abs" >&2
    exit 1
  fi
  if [[ -e "$from_abs" ]]; then
    echo "Renaming directory: $from_abs -> $to_abs"
    mv "$from_abs" "$to_abs"
  fi
}

rename_file_if_needed() {
  local from_abs="$1" to_abs="$2"
  if [[ "$from_abs" == "$to_abs" ]]; then
    return 0
  fi
  if [[ -e "$to_abs" ]]; then
    echo "Cannot rename: destination exists -> $to_abs" >&2
    exit 1
  fi
  if [[ -e "$from_abs" ]]; then
    echo "Renaming file: $from_abs -> $to_abs"
    mv "$from_abs" "$to_abs"
  fi
}

main() {
  echo "Root: $ROOT_DIR"
  echo "Config: $CONFIG_FILE"

  confirm "Proceed with content replacements and renames as per config?"

  do_replacements

  echo "Applying directory/module renames..."

  # Top-level directory renames
  local ANDROID_ROOT_FROM_ABS="$ROOT_DIR/${ANDROID_ROOT_DIR_FROM}"
  local ANDROID_ROOT_TO_ABS="$ROOT_DIR/${ANDROID_ROOT_DIR_TO:-${ANDROID_ROOT_DIR_FROM}}"

  local IOS_FRAMEWORK_ROOT_FROM_ABS="$ROOT_DIR/${IOS_FRAMEWORK_DIR_FROM}"
  local IOS_FRAMEWORK_ROOT_TO_ABS="$ROOT_DIR/${IOS_FRAMEWORK_DIR_TO:-${IOS_FRAMEWORK_DIR_FROM}}"

  local IOS_APP_ROOT_FROM_ABS="$ROOT_DIR/${IOS_APP_DIR_FROM}"
  local IOS_APP_ROOT_TO_ABS="$ROOT_DIR/${IOS_APP_DIR_TO:-${IOS_APP_DIR_FROM}}"

  local UNIAPP_ROOT_FROM_ABS="$ROOT_DIR/${UNIAPP_DIR_FROM}"
  local UNIAPP_ROOT_TO_ABS="$ROOT_DIR/${UNIAPP_DIR_TO:-${UNIAPP_DIR_FROM}}"

  # Perform top-level renames if requested
  if [[ -n "${ANDROID_ROOT_DIR_TO}" && "${ANDROID_ROOT_DIR_TO}" != "${ANDROID_ROOT_DIR_FROM}" ]]; then
    rename_dir_if_needed "$ANDROID_ROOT_FROM_ABS" "$ANDROID_ROOT_TO_ABS"
  fi
  if [[ -n "${IOS_FRAMEWORK_DIR_TO}" && "${IOS_FRAMEWORK_DIR_TO}" != "${IOS_FRAMEWORK_DIR_FROM}" ]]; then
    rename_dir_if_needed "$IOS_FRAMEWORK_ROOT_FROM_ABS" "$IOS_FRAMEWORK_ROOT_TO_ABS"
  fi
  if [[ -n "${IOS_APP_DIR_TO}" && "${IOS_APP_DIR_TO}" != "${IOS_APP_DIR_FROM}" ]]; then
    rename_dir_if_needed "$IOS_APP_ROOT_FROM_ABS" "$IOS_APP_ROOT_TO_ABS"
  fi
  if [[ -n "${UNIAPP_DIR_TO}" && "${UNIAPP_DIR_TO}" != "${UNIAPP_DIR_FROM}" ]]; then
    rename_dir_if_needed "$UNIAPP_ROOT_FROM_ABS" "$UNIAPP_ROOT_TO_ABS"
  fi

  # Determine current effective roots after potential renames
  local ANDROID_ROOT_CUR="$ROOT_DIR/${ANDROID_ROOT_DIR_TO:-${ANDROID_ROOT_DIR_FROM}}"
  local UNIAPP_ROOT_CUR="$ROOT_DIR/${UNIAPP_DIR_TO:-${UNIAPP_DIR_FROM}}"
  local IOS_FRAMEWORK_ROOT_CUR="$ROOT_DIR/${IOS_FRAMEWORK_DIR_TO:-${IOS_FRAMEWORK_DIR_FROM}}"
  local IOS_APP_ROOT_CUR="$ROOT_DIR/${IOS_APP_DIR_TO:-${IOS_APP_DIR_FROM}}"

  # Rename Xcode project bundle directories if project names changed
  if [[ -n "${IOS_FRAMEWORK_PROJECT_NAME_TO}" && "${IOS_FRAMEWORK_PROJECT_NAME_TO}" != "${IOS_FRAMEWORK_PROJECT_NAME_FROM}" ]]; then
    rename_file_if_needed \
      "$IOS_FRAMEWORK_ROOT_CUR/${IOS_FRAMEWORK_PROJECT_NAME_FROM}.xcodeproj" \
      "$IOS_FRAMEWORK_ROOT_CUR/${IOS_FRAMEWORK_PROJECT_NAME_TO}.xcodeproj"
    # Also rename top-level source folder inside if it mirrors project name (common layout)
    rename_dir_if_needed \
      "$IOS_FRAMEWORK_ROOT_CUR/${IOS_FRAMEWORK_PROJECT_NAME_FROM}" \
      "$IOS_FRAMEWORK_ROOT_CUR/${IOS_FRAMEWORK_PROJECT_NAME_TO}"
  fi
  if [[ -n "${IOS_APP_PROJECT_NAME_TO}" && "${IOS_APP_PROJECT_NAME_TO}" != "${IOS_APP_PROJECT_NAME_FROM}" ]]; then
    rename_file_if_needed \
      "$IOS_APP_ROOT_CUR/${IOS_APP_PROJECT_NAME_FROM}.xcodeproj" \
      "$IOS_APP_ROOT_CUR/${IOS_APP_PROJECT_NAME_TO}.xcodeproj"
    rename_dir_if_needed \
      "$IOS_APP_ROOT_CUR/${IOS_APP_PROJECT_NAME_FROM}" \
      "$IOS_APP_ROOT_CUR/${IOS_APP_PROJECT_NAME_TO}"
  fi

  # Android module dir renames inside android root
  if [[ -d "$ANDROID_ROOT_CUR" ]]; then
    local LIB_FROM_DIR="$ANDROID_ROOT_CUR/${ANDROID_LIBRARY_MODULE_DIR_FROM}"
    local LIB_TO_DIR="$ANDROID_ROOT_CUR/${ANDROID_LIBRARY_MODULE_DIR_TO:-${ANDROID_LIBRARY_MODULE_DIR_FROM}}"
    if [[ -n "${ANDROID_LIBRARY_MODULE_DIR_TO}" && "${ANDROID_LIBRARY_MODULE_DIR_TO}" != "${ANDROID_LIBRARY_MODULE_DIR_FROM}" ]]; then
      rename_dir_if_needed "$LIB_FROM_DIR" "$LIB_TO_DIR"
      # Targeted updates for library module name in settings and gradle project refs
      if [[ -f "$ANDROID_ROOT_CUR/settings.gradle.kts" ]]; then
        FROM_MOD="${ANDROID_LIBRARY_MODULE_DIR_FROM}" TO_MOD="${ANDROID_LIBRARY_MODULE_DIR_TO}" \
          perl -0777 -i -pe 'BEGIN { $from=$ENV{FROM_MOD}; $to=$ENV{TO_MOD}; } s/include\((['"'"'"]):$from\1\)/include\(\$1:$to\$1\)/g' \
          "$ANDROID_ROOT_CUR/settings.gradle.kts"
      fi
      find "$ANDROID_ROOT_CUR" -type f \( -name "*.gradle" -o -name "*.gradle.kts" \) -print0 | \
      while IFS= read -r -d $'\0' gf; do
        FROM_MOD="${ANDROID_LIBRARY_MODULE_DIR_FROM}" TO_MOD="${ANDROID_LIBRARY_MODULE_DIR_TO}" \
          perl -0777 -i -pe 'BEGIN { $from=$ENV{FROM_MOD}; $to=$ENV{TO_MOD}; } s/project\((['"'"'"]):$from\1\)/project\(\$1:$to\$1\)/g' "$gf"
      done
    fi

    local APP_FROM_DIR="$ANDROID_ROOT_CUR/${ANDROID_APP_MODULE_DIR_FROM}"
    local APP_TO_DIR="$ANDROID_ROOT_CUR/${ANDROID_APP_MODULE_DIR_TO:-${ANDROID_APP_MODULE_DIR_FROM}}"
    if [[ -n "${ANDROID_APP_MODULE_DIR_TO}" && "${ANDROID_APP_MODULE_DIR_TO}" != "${ANDROID_APP_MODULE_DIR_FROM}" ]]; then
      rename_dir_if_needed "$APP_FROM_DIR" "$APP_TO_DIR"
      # Targeted updates for app module name in settings and gradle project refs
      if [[ -f "$ANDROID_ROOT_CUR/settings.gradle.kts" ]]; then
        FROM_MOD="${ANDROID_APP_MODULE_DIR_FROM}" TO_MOD="${ANDROID_APP_MODULE_DIR_TO}" \
          perl -0777 -i -pe 'BEGIN { $from=$ENV{FROM_MOD}; $to=$ENV{TO_MOD}; } s/include\((['"'"'"]):$from\1\)/include\(\$1:$to\$1\)/g' \
          "$ANDROID_ROOT_CUR/settings.gradle.kts"
      fi
      # Update any project(":app") references under android root
      find "$ANDROID_ROOT_CUR" -type f \( -name "*.gradle" -o -name "*.gradle.kts" \) -print0 | \
      while IFS= read -r -d $'\0' gf; do
        FROM_MOD="${ANDROID_APP_MODULE_DIR_FROM}" TO_MOD="${ANDROID_APP_MODULE_DIR_TO}" \
          perl -0777 -i -pe 'BEGIN { $from=$ENV{FROM_MOD}; $to=$ENV{TO_MOD}; } s/project\((['"'"'"]):$from\1\)/project\(\$1:$to\$1\)/g' "$gf"
      done
    fi

    # Move Android package folders for library and app
    if [[ -d "$LIB_TO_DIR" ]]; then
      move_android_package_dirs "$LIB_TO_DIR" "$ANDROID_LIBRARY_PACKAGE_FROM" "${ANDROID_LIBRARY_PACKAGE_TO:-$ANDROID_LIBRARY_PACKAGE_FROM}"
    fi
    if [[ -d "$APP_TO_DIR" ]]; then
      move_android_package_dirs "$APP_TO_DIR" "$ANDROID_APP_PACKAGE_FROM" "${ANDROID_APP_PACKAGE_TO:-$ANDROID_APP_PACKAGE_FROM}"
    fi
  fi

  # UniApp UTS module directory rename under uni_modules
  local UTS_FROM_ID="$UTS_MODULE_ID_FROM"
  local UTS_TO_ID="${UTS_MODULE_ID_TO:-$UTS_MODULE_ID_FROM}"
  if [[ -d "$UNIAPP_ROOT_CUR" && "$UTS_TO_ID" != "$UTS_FROM_ID" && -n "$UTS_MODULE_ID_TO" ]]; then
    local UTS_FROM_DIR="$UNIAPP_ROOT_CUR/uni_modules/${UTS_FROM_ID}"
    local UTS_TO_DIR="$UNIAPP_ROOT_CUR/uni_modules/${UTS_TO_ID}"
    if [[ -d "$UTS_FROM_DIR" ]]; then
      rename_dir_if_needed "$UTS_FROM_DIR" "$UTS_TO_DIR"
    fi
  fi

  echo "All done. Review changes and rebuild native artifacts where necessary (Android AAR / iOS Framework)."
}

main "$@"
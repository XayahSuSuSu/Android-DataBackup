#!/system/bin/sh

get_storage_space() {
  # $1: path
  df -h "$1" | sed -n 's|% /.*|%|p' | awk '{print $(NF-3),$(NF-2),$(NF)}' | sed 's/G//g' | awk 'END{print ""$2" GB/"$1" GB "$3}'
}

get_apk_path() {
  # $1: packageName
  # $2: user_id
  apk_path="$(find_package "$2" "$1" | cut -f2 -d ':')"
  apk_path="$(echo "$apk_path" | head -1)"
  apk_path="${apk_path%/*}"
  if [ -z "$apk_path" ]; then
    unset apk_path
    return 1
  fi
  echo "$apk_path"
  unset apk_path
}

cd_to_path() {
  # $1: path
  cd "$1" || return 1
}

compress_apk() {
  # $1: compression_type
  # $2: apk_path
  # $3: out_put
  # $4: compatible_mode
  mkdir -p "$3"
  cd_to_path "$2"
  if [ "$4" = "true" ]; then
    case "$1" in
      tar) tar --totals -cf - ./*.apk > "${3}/apk.tar" ;;
      zstd) tar --totals -cf - ./*.apk | $ZSTD_PARA > "${3}/apk.tar.zst" ;;
      lz4) tar --totals -cf - ./*.apk | $LZ4_PARA > "${3}/apk.tar.lz4" ;;
      *) return 1 ;;
    esac
  else
    case "$1" in
      tar) tar --totals -cf "${3}/apk.tar" ./*.apk ;;
      zstd) tar --totals -cf "${3}/apk.tar.zst" ./*.apk -I "$ZSTD_PARA" ;;
      lz4) tar --totals -cf "${3}/apk.tar.lz4" ./*.apk -I "$LZ4_PARA" ;;
      *) return 1 ;;
    esac
  fi
  code=$?
  cd_to_path "/"
  return $code
}

compress() {
  # $1: compression_type
  # $2: data_type
  # $3: package_name
  # $4: out_put
  # $5: data_path
  # $6: compatible_mode
  mkdir -p "$4"
  am force-stop "$3"
  case "$2" in
    user | user_de)
      if [ -d "$5/$3" ]; then
        if [ "$6" = "true" ]; then
          case "$1" in
            tar) tar --totals --exclude="$3/.ota" --exclude="$3/cache" --exclude="$3/lib" --exclude="$3/code_cache" --exclude="$3/no_backup" -cpf - -C "$5" "$3" > "$4/$2.tar" ;;
            zstd) tar --totals --exclude="$3/.ota" --exclude="$3/cache" --exclude="$3/lib" --exclude="$3/code_cache" --exclude="$3/no_backup" -cpf - -C "$5" "$3" | $ZSTD_PARA > "$4/$2.tar.zst" ;;
            lz4) tar --totals --exclude="$3/.ota" --exclude="$3/cache" --exclude="$3/lib" --exclude="$3/code_cache" --exclude="$3/no_backup" -cpf - -C "$5" "$3" | $LZ4_PARA > "$4/$2.tar.lz4" ;;
          esac
        else
          case "$1" in
            tar) tar --totals --exclude="$3/.ota" --exclude="$3/cache" --exclude="$3/lib" --exclude="$3/code_cache" --exclude="$3/no_backup" -cpf "$4/$2.tar" -C "$5" "$3" ;;
            zstd) tar --totals --exclude="$3/.ota" --exclude="$3/cache" --exclude="$3/lib" --exclude="$3/code_cache" --exclude="$3/no_backup" -cpf "$4/$2.tar.zst" -C "$5" "$3" -I "$ZSTD_PARA" ;;
            lz4) tar --totals --exclude="$3/.ota" --exclude="$3/cache" --exclude="$3/lib" --exclude="$3/code_cache" --exclude="$3/no_backup" -cpf "$4/$2.tar.lz4" -C "$5" "$3" -I "$LZ4_PARA" ;;
          esac
        fi
      else
        echo "No such path: $5"
        return 1
      fi
      ;;
    data | obb)
      if [ -d "$5/$3" ]; then
        if [ "$6" = "true" ]; then
          case "$1" in
            tar) tar --totals --exclude="Backup_"* --exclude="$3/cache" -cpf - -C "$5" "$3" > "$4/$2.tar" ;;
            zstd) tar --totals --exclude="Backup_"* --exclude="$3/cache" -cpf - -C "$5" "$3" | $ZSTD_PARA > "$4/$2.tar.zst" ;;
            lz4) tar --totals --exclude="Backup_"* --exclude="$3/cache" -cpf - -C "$5" "$3" | $LZ4_PARA > "$4/$2.tar.lz4" ;;
          esac
        else
          case "$1" in
            tar) tar --totals --exclude="Backup_"* --exclude="$3/cache" -cpf "$4/$2.tar" -C "$5" "$3" ;;
            zstd) tar --totals --exclude="Backup_"* --exclude="$3/cache" -cpf "$4/$2.tar.zst" -C "$5" "$3" -I "$ZSTD_PARA" ;;
            lz4) tar --totals --exclude="Backup_"* --exclude="$3/cache" -cpf "$4/$2.tar.lz4" -C "$5" "$3" -I "$LZ4_PARA" ;;
          esac
        fi
      else
        echo "No such path: $5/$3"
        return 1
      fi
      ;;
    media)
      if [ -d "$5" ]; then
        write_to_file "$5" "$5/com.xayah.databackup.PATH"
        if [ "$6" = "true" ]; then
          case "$1" in
            tar) tar --totals --exclude="Backup_"* --exclude="${5##*/}/cache" -cpf - -C "${5%/*}" "${5##*/}" > "$4/${5##*/}.tar" ;;
            zstd) tar --totals --exclude="Backup_"* --exclude="${5##*/}/cache" -cpf - -C "${5%/*}" "${5##*/}" | $ZSTD_PARA > "$4/${5##*/}.tar.zst" ;;
            lz4) tar --totals --exclude="Backup_"* --exclude="${5##*/}/cache" -cpf - -C "${5%/*}" "${5##*/}" | $LZ4_PARA > "$4/${5##*/}.tar.lz4" ;;
          esac
        else
          case "$1" in
            tar) tar --totals --exclude="Backup_"* --exclude="${5##*/}/cache" -cpf "$4/${5##*/}.tar" -C "${5%/*}" "${5##*/}" ;;
            zstd) tar --totals --exclude="Backup_"* --exclude="${5##*/}/cache" -cpf "$4/${5##*/}.tar.zst" -C "${5%/*}" "${5##*/}" -I "$ZSTD_PARA" ;;
            lz4) tar --totals --exclude="Backup_"* --exclude="${5##*/}/cache" -cpf "$4/${5##*/}.tar.lz4" -C "${5%/*}" "${5##*/}" -I "$LZ4_PARA" ;;
          esac
        fi
        code=$?
        rm -rf "$5/com.xayah.databackup.PATH"
        return $code
      else
        echo "No such path: $5"
        return 1
      fi
      ;;
  esac
}

set_install_env() {
  settings put global verifier_verify_adb_installs 0
  settings put global package_verifier_enable 0
  if [ "$(settings get global package_verifier_user_consent)" != -1 ]; then
    settings put global package_verifier_user_consent -1
    settings put global upload_apk_enable 0
  fi
}

install_apk() {
  # $1: in_path
  # $2: package_name
  # $3: user_id
  tmp_dir="/data/local/tmp/data_backup"
  rm -rf "$tmp_dir"
  mkdir -p "$tmp_dir"
  find "$1" -maxdepth 1 -name "apk.*" -type f | while read -r i; do
    case "${i##*.}" in
      tar) tar --totals -xmpf "$i" -C "$tmp_dir" ;;
      zst | lz4) tar --totals -I "zstd" -xmpf "$i" -C "$tmp_dir" ;;
    esac
  done
  apk_num=$(find "$tmp_dir" -maxdepth 1 -name "*.apk" -type f | wc -l)
  case "$apk_num" in
    0) return 1 ;;
    1) pm_install "$3" ${tmp_dir}/*.apk ;;
    *)
      session=$(pm_install_create "$3" | grep -E -o '[0-9]+')
      find "$tmp_dir" -maxdepth 1 -name "*.apk" -type f | while read -r i; do
        pm install-write "$session" "${i##*/}" "$i"
      done
      pm install-commit "$session"
      ;;
  esac
  code=$?
  rm -rf "$tmp_dir"
  return $code
}

pm_install() {
  # $1: user_id
  # $2: apk_path
  if [ "$(getprop ro.build.version.sdk)" -lt 30 ]; then
    pm install --user "$1" -r -t "$2"
  else
    pm install -i com.android.vending --user "$1" -r -t "$2"
  fi
}

pm_install_create() {
  # $1: user_id
  if [ "$(getprop ro.build.version.sdk)" -lt 30 ]; then
    pm install-create --user "$1" -t
  else
    pm install-create -i com.android.vending --user "$1" -t
  fi
}

get_SELinux_context() {
  # $1: path
  ls -Zd "$1" | awk 'NF>1{print $1}'
}

set_owner_and_SELinux() {
  # $1: data_type
  # $2: package_name
  # $3: path
  # $4: user_id
  # $5: support_fix_context
  # $6: context
  if [ -f /config/sdcardfs/"$2"/appid ]; then
    owner="$(cat "/config/sdcardfs/$2/appid")"
  else
    owner="$(dumpsys package "$2" | grep -w 'userId' | head -1)"
  fi
  owner="$(echo "$owner" | grep -E -o '[0-9]+')"
  if [ "$owner" != "" ]; then
    chown -hR "$4$owner:$4$owner" "$3/"
    if [ "$1" = "user" ]; then
      restorecon -RFD "$3/"
      if [ "$5" = "true" ]; then
        if [ "$6" != "" ]; then
          chcon -hR "$6" "$3/"
        else
          context=$(ls -Zd "$3/../" | awk 'NF>1{print $1}' | sed -e "s/system_data_file/app_data_file/g")
          chcon -hR "$context" "$3/"
        fi
      fi
    fi
  fi
}

decompress() {
  # $1: compression_type
  # $2: data_type
  # $3: input_path
  # $4: package_name
  # $5: data_path
  am force-stop "$4"
  case "$2" in
    media)
      tmp_dir="/data/local/tmp/data_backup"
      path_file_name='com.xayah.databackup.PATH'
      mkdir "$tmp_dir"
      case "$1" in
        tar)
          tar -xpf "$3" -C "$tmp_dir" --wildcards --no-anchored "$path_file_name"
          data_path=$(cat "$tmp_dir/$4/$path_file_name")
          if [ "$data_path" != "" ]; then
            tar --totals --recursive-unlink -xpf "$3" -C "${data_path%/*}"
            rm -rf "${data_path:?}/$path_file_name"
          else
            return 1
          fi
          ;;
        lz4 | zstd)
          tar -I "zstd" -xpf "$3" -C "$tmp_dir" --wildcards --no-anchored "$path_file_name"
          data_path=$(cat "$tmp_dir/$4/$path_file_name")
          if [ "$data_path" != "" ]; then
            tar --totals --recursive-unlink -I "zstd" -xpf "$3" -C "${data_path%/*}"
            rm -rf "${data_path:?}/$path_file_name"
          else
            return 1
          fi
          ;;
      esac
      code=$?
      rm -rf "$tmp_dir"
      return $code
      ;;
    *)
      case "$1" in
        tar) tar --totals --recursive-unlink -xmpf "$3" -C "$5" ;;
        lz4 | zstd) tar --totals --recursive-unlink -I "zstd" -xmpf "$3" -C "$5" ;;
      esac
      ;;
  esac
}

get_app_version() {
  # $1: package_name
  dumpsys package "$1" | awk '/versionName=/{print $1}' | cut -f2 -d '=' | head -1
}

get_app_version_code() {
  # $1: user_id
  # $2: package_name
  pm list packages --show-versioncode --user "$1" "$2" | cut -f3 -d ':' | head -n 1
}

write_to_file() {
  # $1: content
  # $2: path
  echo "$1" > "$2"
}

check_bashrc() {
  echo "OK"
}

test_archive() {
  # $1: compression_type
  # $2: input_path
  if [ -e "$2" ]; then
    case "$1" in
      tar) tar -t -f "$2" > /dev/null 2>&1 ;;
      zstd | lz4) tar -t -f "$2" -I "zstd" > /dev/null 2>&1 ;;
    esac
  else
    echo "No such path: $2"
    return 1
  fi
}

list_users() {
  ls -1 "/data/user"
}

list_packages() {
  # $1: user_id
  pm list packages --user "$1"
}

find_package() {
  # $1: user_id
  # $2: package_name
  if [ -z "$1" ]; then
    pm path "$2"
  else
    pm path --user "$1" "$2"
  fi
}

count_size() {
  # $1: path
  # $2: type
  if [ -e "$1" ]; then
    case "$2" in
      0) du -ksh "$1" | awk '{print $1}' ;;
      *) du -ks "$1" | awk '{print $1}' ;;
    esac
  fi
}

list_external_storage() {
  mount | awk '$3 ~ /\mnt\/media_rw/ {print $3, $5}'
}

get_keyboard() {
  settings get secure default_input_method
}

set_keyboard() {
  # $1: keyboard
  if [ "$1" != "" ]; then
     ime enable "$1"
     ime set "$1"
     settings put secure default_input_method "$1"
  fi
}

get_accessibility_services() {
  settings get secure enabled_accessibility_services
}

set_accessibility_services() {
  # $1: services
  if [ "$1" != "" ]; then
     if [ "$1" != null ]; then
      settings put secure enabled_accessibility_services "$1"
      settings put secure accessibility_enabled 1
    fi
  fi
}

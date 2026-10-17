#!/bin/sh
# Device-side tar/zstd round trip.
# Usage: tar_roundtrip.sh TAR ZSTD
#
# Archive, list, and extract a tiny tree (an empty file and an icon directory)
# directly and through zstd. Each pipeline stage is checked on its own so a
# downstream success cannot hide an upstream error. A truncated archive must
# fail tar -tf.

set -eu

if [ "$#" -ne 2 ]; then
    echo "usage: tar_roundtrip.sh TAR ZSTD" >&2
    exit 2
fi

TAR=$1
ZSTD=$2

if [ ! -f "$TAR" ] || [ ! -f "$ZSTD" ]; then
    echo "tar and zstd must be file paths" >&2
    exit 2
fi

"$TAR" --version >/dev/null
"$ZSTD" --version >/dev/null

work=${TMPDIR:-/tmp}/tar-roundtrip.$$
rm -rf "$work"
mkdir -p "$work"
trap 'rm -rf "$work"' EXIT INT HUP TERM

# $1 is the upstream command (a function). The remaining arguments are the
# downstream command. Neither status is ignored.
run_pipe() {
    upstream=$1
    shift
    status_file=$work/pipe.status
    rm -f "$status_file"
    set +e
    {
        set +e
        "$upstream"
        echo $? > "$status_file"
    } | "$@"
    downstream_rc=$?
    set -e
    if [ ! -f "$status_file" ]; then
        echo "pipeline failed: $upstream reported no status" >&2
        exit 1
    fi
    upstream_rc=$(cat "$status_file")
    if [ "$upstream_rc" -ne 0 ] || [ "$downstream_rc" -ne 0 ]; then
        echo "pipeline failed: $upstream=$upstream_rc downstream=$downstream_rc" >&2
        exit 1
    fi
}

src=$work/src
mkdir -p "$src/icon/com.example"
: > "$src/empty"
printf 'icon-bytes' > "$src/icon/com.example/icon.png"

cmp_tree() {
    from=$1
    to=$2
    (cd "$from" && find . | sort) > "$work/tree.from"
    (cd "$to" && find . | sort) > "$work/tree.to"
    cmp "$work/tree.from" "$work/tree.to"
    while IFS= read -r rel; do
        if [ -f "$from/$rel" ]; then
            cmp "$from/$rel" "$to/$rel"
        fi
    done < "$work/tree.from"
}

echo "direct archive"
"$TAR" --totals -cpf "$work/direct.tar" -C "$src" .
"$TAR" -tf "$work/direct.tar" > "$work/direct.list"
grep -q 'empty' "$work/direct.list"
grep -q 'icon/com.example/icon.png' "$work/direct.list"
mkdir -p "$work/direct.out"
"$TAR" --totals -xmpf "$work/direct.tar" -C "$work/direct.out"
cmp_tree "$src" "$work/direct.out"

tar_to_stdout() {
    "$TAR" --totals -cpf - -C "$src" .
}

echo "zstd archive"
run_pipe tar_to_stdout "$ZSTD" -c > "$work/archive.tar.zst"

zstd_decompress() {
    "$ZSTD" -d -c "$work/archive.tar.zst"
}

echo "zstd list"
run_pipe zstd_decompress "$TAR" -tf - > "$work/zstd.list"
grep -q 'empty' "$work/zstd.list"
grep -q 'icon/com.example/icon.png' "$work/zstd.list"

echo "zstd extract"
mkdir -p "$work/zstd.out"
run_pipe zstd_decompress "$TAR" --totals -xmpf - -C "$work/zstd.out"
cmp_tree "$src" "$work/zstd.out"

echo "truncated archive"
dd if="$work/direct.tar" of="$work/trunc.tar" bs=128 count=1 2>/dev/null
set +e
"$TAR" -tf "$work/trunc.tar" >/dev/null 2>"$work/trunc.err"
trunc_rc=$?
set -e
if [ "$trunc_rc" -eq 0 ]; then
    echo "truncated archive passed validation" >&2
    exit 1
fi

echo "round trip ok"
exit 0

#!/bin/bash
# Builds linphone-sdk 5.2 (arm64 only) for BcsPhone Android in the Belledonne r20 Docker image.
# Usage: sdk/build-sdk.sh [path to linphone-sdk] (default: ../linphone-sdk next to this repository)
# See sdk/README-build.md for the sources to prepare first.
set -e

SDK_DIR="$(realpath "${1:-$(dirname "$0")/../../linphone-sdk}")"
IMAGE=gitlab.linphone.org:4567/bc/public/linphone-sdk/bc-dev-android-r20:20210914_update_java11

if [ -e "$SDK_DIR/build" ]; then
	echo "$SDK_DIR/build already exists: rename it first" >&2
	exit 1
fi

docker run --rm --volume="$SDK_DIR":/home/bc/linphone-sdk -w /home/bc/linphone-sdk "$IMAGE" bash -c '
mkdir build && cd build &&
cmake .. -DLINPHONESDK_PLATFORM=Android -DLINPHONESDK_ANDROID_ARCHS=arm64 -DCMAKE_BUILD_TYPE=RelWithDebInfo \
	-DENABLE_ADVANCED_IM=OFF -DENABLE_LIME_X3DH=OFF -DENABLE_ZRTP=OFF -DENABLE_LDAP=OFF -DENABLE_MKV=OFF \
	-DENABLE_ILBC=OFF -DENABLE_ISAC=OFF &&
cmake --build . -j$(nproc)'

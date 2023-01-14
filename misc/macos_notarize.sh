#!/bin/sh

set -e

APP_DIR="$1/build/dist/Pdx-Unlimiter.app"
ARCHIVE="$TMPDIR/notarization.zip"

mv -f "$APP_DIR/Contents/Resources/resources/bin/rakaly_mac" "$APP_DIR/Contents/MacOS/rakaly_mac" || true
codesign -vvv --deep --entitlements "$1/misc/Entitlements.plist" --options=runtime --force --strict --sign "Developer ID Application: Christopher Schnick (PF6V9HYACS)" "$APP_DIR/Contents/MacOS/Pdx-Unlimiter"
codesign -vvv --deep --options=runtime --force --strict --sign "Developer ID Application: Christopher Schnick (PF6V9HYACS)" "$APP_DIR/Contents/MacOS/rakaly_mac"


echo "Create keychain profile"
xcrun notarytool store-credentials "notarytool-profile" --apple-id "$MAC_NOTARIZATION_APPLE_ID" --team-id "$MAC_NOTARIZATION_TEAM_ID" --password "$MAC_APP_SPECIFIC_PASSWORD"

# We can't notarize an app bundle directly, but we need to compress it as an archive.
# Therefore, we create a zip file containing our app bundle, so that we can send it to the
# notarization service

echo "Creating temp notarization archive"
ditto -c -k --sequesterRsrc --keepParent "$APP_DIR" "$ARCHIVE"

# Here we send the notarization request to the Apple's Notarization service, waiting for the result.
# This typically takes a few seconds inside a CI environment, but it might take more depending on the App
# characteristics. Visit the Notarization docs for more information and strategies on how to optimize it if
# you're curious

echo "Notarize app"
ID=$(xcrun notarytool submit "$ARCHIVE" --keychain-profile "notarytool-profile" --wait | grep -o -i -E '[0-9A-F]{8}-[0-9A-F]{4}-[4][0-9A-F]{3}-[89AB][0-9A-F]{3}-[0-9A-F]{12}' | head -1)
echo "Submission ID is $ID"
xcrun notarytool log "$ID" --keychain-profile "notarytool-profile"

# Finally, we need to "attach the staple" to our executable, which will allow our app to be
# validated by macOS even when an internet connection is not available.
echo "Attach staple"
xcrun stapler staple "$APP_DIR"
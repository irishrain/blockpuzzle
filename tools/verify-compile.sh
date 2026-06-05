#!/usr/bin/env bash
#
# Offline compile / type-check of the whole app.
#
# A real `./gradlew assembleDebug` is not possible in every environment because
# the Android SDK, the Android Gradle Plugin and the AndroidX libraries are only
# served from Google's servers (dl.google.com / maven.google.com). When those
# hosts are blocked, this script still type-checks every Java + Kotlin source in
# app/src by compiling them against:
#
#   * the full Android framework, taken from Robolectric's `android-all` jar
#     (published to Maven Central), and
#   * tiny local stubs for the handful of AndroidX symbols and the generated
#     R / view-binding classes the code refers to.
#
# Everything it needs comes from Maven Central, so it works behind a network
# policy that only allows repo1.maven.org.
#
# Usage:  tools/verify-compile.sh
# Exit code is non-zero if anything fails to compile.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="$ROOT/.verify"
LIBS="$WORK/libs"
STUBS="$WORK/stubs"
OUT="$WORK/out"
MC="https://repo1.maven.org/maven2"
KOTLIN_VERSION="1.9.24"
ANDROID_ALL="11-robolectric-6757853"   # Android 11 == API 30 == compileSdkVersion

mkdir -p "$LIBS" "$STUBS" "$OUT/kt" "$OUT/java"

fetch() { # url dest
  if [ ! -s "$2" ]; then
    echo "  downloading $(basename "$2")"
    curl -fsSL -m 180 -o "$2" "$1"
  fi
}

echo "==> Fetching dependencies from Maven Central"
fetch "$MC/org/jetbrains/kotlin/kotlin-compiler/$KOTLIN_VERSION/kotlin-compiler-$KOTLIN_VERSION.jar"           "$LIBS/kotlin-compiler.jar"
fetch "$MC/org/jetbrains/kotlin/kotlin-stdlib/$KOTLIN_VERSION/kotlin-stdlib-$KOTLIN_VERSION.jar"               "$LIBS/kotlin-stdlib.jar"
fetch "$MC/org/jetbrains/kotlin/kotlin-reflect/$KOTLIN_VERSION/kotlin-reflect-$KOTLIN_VERSION.jar"             "$LIBS/kotlin-reflect.jar"
fetch "$MC/org/jetbrains/kotlin/kotlin-script-runtime/$KOTLIN_VERSION/kotlin-script-runtime-$KOTLIN_VERSION.jar" "$LIBS/kotlin-script-runtime.jar"
fetch "$MC/org/jetbrains/intellij/deps/trove4j/1.0.20200330/trove4j-1.0.20200330.jar"                         "$LIBS/trove4j.jar"
fetch "$MC/org/jetbrains/annotations/23.0.0/annotations-23.0.0.jar"                                           "$LIBS/jetbrains-annotations.jar"
fetch "$MC/com/google/code/gson/gson/2.8.6/gson-2.8.6.jar"                                                    "$LIBS/gson.jar"
fetch "$MC/junit/junit/4.13.2/junit-4.13.2.jar"                                                               "$LIBS/junit.jar"
fetch "$MC/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"                                              "$LIBS/hamcrest.jar"
fetch "$MC/org/robolectric/android-all/$ANDROID_ALL/android-all-$ANDROID_ALL.jar"                            "$LIBS/android-all.jar"

echo "==> Generating local stubs (AndroidX + R + view bindings)"
mkdir -p "$STUBS/androidx/annotation" "$STUBS/androidx/appcompat/app" \
         "$STUBS/androidx/core/content" "$STUBS/de/mwvb/blockpuzzle" "$STUBS/kt"

for ann in NonNull Nullable ColorInt; do
cat > "$STUBS/androidx/annotation/$ann.java" <<EOF
package androidx.annotation;
import java.lang.annotation.*;
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD,ElementType.PARAMETER,ElementType.FIELD,ElementType.LOCAL_VARIABLE,ElementType.TYPE_USE})
public @interface $ann {}
EOF
done
cat > "$STUBS/androidx/annotation/RequiresApi.java" <<'EOF'
package androidx.annotation;
import java.lang.annotation.*;
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD,ElementType.TYPE,ElementType.CONSTRUCTOR})
public @interface RequiresApi { int value() default 1; int api() default 1; }
EOF
cat > "$STUBS/androidx/appcompat/app/AppCompatActivity.java" <<'EOF'
package androidx.appcompat.app;
public class AppCompatActivity extends android.app.Activity {}
EOF
cat > "$STUBS/androidx/appcompat/app/AlertDialog.java" <<'EOF'
package androidx.appcompat.app;
import android.content.Context;
import android.content.DialogInterface;
public class AlertDialog {
    public static class Builder {
        public Builder(Context c) {}
        public Builder setTitle(int r) { return this; }
        public Builder setTitle(CharSequence c) { return this; }
        public Builder setPositiveButton(int r, DialogInterface.OnClickListener l) { return this; }
        public Builder setPositiveButton(CharSequence c, DialogInterface.OnClickListener l) { return this; }
        public Builder setNegativeButton(int r, DialogInterface.OnClickListener l) { return this; }
        public Builder setNegativeButton(CharSequence c, DialogInterface.OnClickListener l) { return this; }
        public AlertDialog show() { return new AlertDialog(); }
    }
}
EOF
cat > "$STUBS/androidx/core/content/ContextCompat.java" <<'EOF'
package androidx.core.content;
import android.content.Context;
public class ContextCompat { public static int getColor(Context c, int id) { return 0; } }
EOF

# Generated R class, derived from the resource ids the code actually references.
R="$STUBS/de/mwvb/blockpuzzle/R.java"
{
  echo "package de.mwvb.blockpuzzle;"
  echo "public final class R {"
  for t in string color layout raw drawable id mipmap style; do
    echo "  public static final class $t {"
    i=0
    for n in $(grep -rhoE "R\.$t\.[A-Za-z0-9_]+" "$ROOT/app/src/main/java" --include=*.kt --include=*.java \
               | sed -E "s/R\.$t\.//" | sort -u); do
      echo "    public static final int $n = $i;"; i=$((i+1))
    done
    echo "  }"
  done
  echo "}"
} > "$R"

# View-binding (kotlinx synthetic) stub for activity_main, used by MainActivity.
cat > "$STUBS/kt/activity_main_synthetic.kt" <<'EOF'
package kotlinx.android.synthetic.main.activity_main
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import de.mwvb.blockpuzzle.playingfield.PlayingFieldView
val AppCompatActivity.placeholder1: View get() = TODO()
val AppCompatActivity.placeholder2: View get() = TODO()
val AppCompatActivity.placeholder3: View get() = TODO()
val AppCompatActivity.parking: View get() = TODO()
val AppCompatActivity.playingField: PlayingFieldView get() = TODO()
val AppCompatActivity.newGame: Button get() = TODO()
val AppCompatActivity.info: TextView get() = TODO()
val AppCompatActivity.infoDisplay: TextView get() = TODO()
val AppCompatActivity.territoryName: TextView get() = TODO()
EOF

DEPCP="$LIBS/android-all.jar:$LIBS/kotlin-stdlib.jar:$LIBS/kotlin-reflect.jar:$LIBS/gson.jar:$LIBS/jetbrains-annotations.jar:$LIBS/junit.jar:$LIBS/hamcrest.jar"
RUNCP="$LIBS/kotlin-compiler.jar:$LIBS/kotlin-stdlib.jar:$LIBS/kotlin-reflect.jar:$LIBS/kotlin-script-runtime.jar:$LIBS/trove4j.jar:$LIBS/jetbrains-annotations.jar"

find "$ROOT/app/src/main/java" "$ROOT/app/src/test/java" "$STUBS" -name '*.java' > "$WORK/java.list"
find "$ROOT/app/src/main/java" "$ROOT/app/src/test/java" "$STUBS/kt" -name '*.kt'  > "$WORK/kt.list"
echo "==> Sources: $(wc -l < "$WORK/kt.list") Kotlin, $(wc -l < "$WORK/java.list") Java"

echo "==> Compiling Kotlin"
rm -rf "$OUT/kt"; mkdir -p "$OUT/kt"
java -cp "$RUNCP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-stdlib -jvm-target 1.8 -language-version 1.9 \
  -classpath "$DEPCP" -d "$OUT/kt" \
  "@$WORK/kt.list" "@$WORK/java.list"

echo "==> Compiling Java"
rm -rf "$OUT/java"; mkdir -p "$OUT/java"
javac -nowarn -proc:none -source 8 -target 8 \
  -cp "$DEPCP:$OUT/kt" -d "$OUT/java" "@$WORK/java.list"

echo
echo "==> SUCCESS: all Java + Kotlin sources type-check."

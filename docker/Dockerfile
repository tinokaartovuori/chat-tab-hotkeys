# GUI-capable JDK image for `./rl run`.
#
# The slim eclipse-temurin JDK ships no X11/AWT native libs, so RuneLite's
# Toolkit.<clinit> fails with `UnsatisfiedLinkError: libXext.so.6`. This adds
# the X11 client libs, OpenGL, and a base font so the RuneLite window can open
# and render. Built once by the `rl` wrapper and cached; nothing is installed
# on the host.
FROM eclipse-temurin:11-jdk

RUN apt-get update && apt-get install -y --no-install-recommends \
        libxext6 \
        libxrender1 \
        libxtst6 \
        libxi6 \
        libxrandr2 \
        libxxf86vm1 \
        libfreetype6 \
        fontconfig \
        fonts-dejavu-core \
        libgl1 \
    && rm -rf /var/lib/apt/lists/*

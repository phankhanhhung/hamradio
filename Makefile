# Ham Radio Simulation — Build System
JAVA_HOME   ?= $(shell readlink -f $$(which javac) | sed 's|/bin/javac||')
JAVA_INC     = $(JAVA_HOME)/include
JAVA_INC_OS  = $(JAVA_INC)/linux

SRC_JAVA     = src/main/java
SRC_C        = src/main/c
BUILD        = build
LIB          = lib
CLASSES      = $(BUILD)/classes
HEADERS      = $(SRC_C)

JAVAC        = javac
JAVA         = java
CC           = gcc
CFLAGS       = -shared -fPIC -O2 -Wall -Wno-unused-function
LDFLAGS      = -lm

NATIVE_LIB   = $(LIB)/libhamradio.so

# C source files
C_SOURCES    = $(SRC_C)/hamradio_buffer.c \
               $(SRC_C)/jni_buffer.c

# JNI classes that need header generation
JNI_CLASSES  = com.hamradio.dsp.buffer.NativeBuffer

# Classpath
CP           = $(CLASSES)

.PHONY: all java headers native clean

all: java headers native

# --- Java compilation ---
java:
	@mkdir -p $(CLASSES)
	$(JAVAC) -d $(CLASSES) \
		$$(find $(SRC_JAVA) -name "*.java")
	@echo "[make] Java classes compiled."

# --- Generate JNI headers ---
headers: java
	@for cls in $(JNI_CLASSES); do \
		src_file=$$(echo $$cls | tr '.' '/'); \
		$(JAVAC) -h $(HEADERS) -d $(CLASSES) $(SRC_JAVA)/$$src_file.java; \
	done
	@echo "[make] JNI headers generated."

# --- Native C library ---
native: headers
	@mkdir -p $(LIB)
	$(CC) $(CFLAGS) \
		-I$(JAVA_INC) -I$(JAVA_INC_OS) -I$(SRC_C) \
		$(C_SOURCES) \
		-o $(NATIVE_LIB) $(LDFLAGS)
	@echo "[make] Native library built: $(NATIVE_LIB)"

# --- Clean ---
clean:
	rm -rf $(BUILD) $(LIB)
	@echo "[make] Cleaned."

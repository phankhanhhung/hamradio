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

# JavaFX
JAVAFX_SDK   = javafx-sdk
JAVAFX_LIB   = $(JAVAFX_SDK)/lib
JAVAFX_MODS  = javafx.controls,javafx.fxml

# Dependencies
DEPS         = deps
SQLITE_JAR   = $(DEPS)/sqlite-jdbc-3.45.1.0.jar
SLF4J_JARS   = $(DEPS)/slf4j-api-2.0.9.jar:$(DEPS)/slf4j-nop-2.0.9.jar
DEP_CP       = $(SQLITE_JAR):$(SLF4J_JARS)

# C source files
C_SOURCES    = $(SRC_C)/hamradio_buffer.c \
               $(SRC_C)/hamradio_dsp.c \
               $(SRC_C)/hamradio_modem.c \
               $(SRC_C)/hamradio_rf.c \
               $(SRC_C)/jni_dsp.c \
               $(SRC_C)/jni_rf.c \
               $(SRC_C)/jni_buffer.c

# JNI classes that need header generation
JNI_CLASSES  = com.hamradio.dsp.NativeDSP \
               com.hamradio.rf.NativeRF \
               com.hamradio.dsp.buffer.NativeBuffer

# Classpath
CP           = $(CLASSES)
RUN_CP       = $(CLASSES)

.PHONY: all java headers native run clean deps

all: deps java headers native

# --- Dependencies ---
deps:
	@mkdir -p $(DEPS)
	@if [ ! -f "$(SQLITE_JAR)" ]; then \
		echo "[make] Downloading SQLite JDBC..."; \
		curl -sL "https://github.com/xerial/sqlite-jdbc/releases/download/3.45.1.0/sqlite-jdbc-3.45.1.0.jar" -o "$(SQLITE_JAR)"; \
	fi
	@if [ ! -f "$(DEPS)/slf4j-api-2.0.9.jar" ]; then \
		echo "[make] Downloading SLF4J..."; \
		curl -sL "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar" -o "$(DEPS)/slf4j-api-2.0.9.jar"; \
		curl -sL "https://repo1.maven.org/maven2/org/slf4j/slf4j-nop/2.0.9/slf4j-nop-2.0.9.jar" -o "$(DEPS)/slf4j-nop-2.0.9.jar"; \
	fi

# --- Java compilation ---
java: deps
	@mkdir -p $(CLASSES)
	$(JAVAC) --module-path $(JAVAFX_LIB) --add-modules $(JAVAFX_MODS) \
		-cp $(DEP_CP) \
		-d $(CLASSES) \
		$$(find $(SRC_JAVA) -name "*.java")
	@echo "[make] Java classes compiled."

# --- Generate JNI headers ---
headers: java
	@for cls in $(JNI_CLASSES); do \
		src_file=$$(echo $$cls | tr '.' '/'); \
		$(JAVAC) --module-path $(JAVAFX_LIB) --add-modules $(JAVAFX_MODS) \
			-cp $(DEP_CP) \
			-h $(HEADERS) -d $(CLASSES) $(SRC_JAVA)/$$src_file.java; \
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

# --- Run JavaFX GUI (single-process mode) ---
run: all
	$(JAVA) --module-path $(JAVAFX_LIB) --add-modules $(JAVAFX_MODS) \
		-Djava.library.path=$(LIB) \
		-cp "$(CLASSES):$(DEP_CP)" \
		com.hamradio.HamRadioApp

# --- Run Server (headless, no JavaFX) ---
run-server: all
	$(JAVA) -Djava.library.path=$(LIB) \
		-cp "$(CLASSES):$(DEP_CP)" \
		com.hamradio.server.HamRadioServer $(ARGS)

# --- Run Client (JavaFX GUI, connects to server) ---
run-client: all
	$(JAVA) --module-path $(JAVAFX_LIB) --add-modules $(JAVAFX_MODS) \
		-Djava.library.path=$(LIB) \
		-cp "$(CLASSES):$(DEP_CP)" \
		com.hamradio.client.HamRadioClient

# --- Clean ---
clean:
	rm -rf $(BUILD) $(LIB)
	rm -f $(SRC_C)/com_hamradio_*.h
	@echo "[make] Cleaned."

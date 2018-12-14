SRC_DIR = src
OUT_DIR = exe
CP = include/*:.
OPTS = -Xlint:unchecked
SRC_FILES = $(shell find ${SRC_DIR} -name "*.java")
JAVA = javac
CFLAGS = -Wno-sign-compare
all:
	${JAVA} -d ${OUT_DIR} -sourcepath ${SRC_DIR} -cp ${CP} ${OPTS} ${MAIN} ${SRC_FILES}

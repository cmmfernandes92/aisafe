CC=gcc
CFLAGS=-Wall -Wextra -g -std=c99 -D_DEFAULT_SOURCE
LDLIBS=-lm -pthread
# -lrt: necessário em Linux para shm_open/sem_open (glibc < 2.17)
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Linux)
    LDLIBS += -lrt
endif

OBJS=flight_sim.o flight_proc.o violation.o report_proc.o config.o json.o movement.o report_thread.o detection_thread.o environment_thread.o

flight_sim: $(OBJS)
	$(CC) $(CFLAGS) $(OBJS) -o flight_sim $(LDLIBS)

flight_sim.o: flight_sim.c types.h detection_thread.h report_thread.h environment_thread.h
flight_proc.o: flight_proc.c types.h movement.h
environment_thread.o: environment_thread.c environment_thread.h types.h
violation.o: violation.c types.h
report_proc.o: report_proc.c types.h
config.o: config.c types.h json.h movement.h
json.o: json.c types.h json.h
movement.o: movement.c types.h movement.h
report_thread.o: report_thread.c report_thread.h types.h
detection_thread.o: detection_thread.c detection_thread.h report_thread.h types.h

test_us105: test_us105.c types.h
	$(CC) $(CFLAGS) -o test_us105 test_us105.c $(LDLIBS)

test_scenarios: test_scenarios.c flight_sim
	$(CC) $(CFLAGS) -o test_scenarios test_scenarios.c

test_us106: test_us106.c flight_sim
	$(CC) $(CFLAGS) -o test_us106 test_us106.c

test_us107: test_us107.c report_thread.o report_proc.o violation.o types.h
	$(CC) $(CFLAGS) -o test_us107 test_us107.c report_thread.o report_proc.o violation.o $(LDLIBS)

test_us108: test_us108.c types.h
	$(CC) $(CFLAGS) -o test_us108 test_us108.c $(LDLIBS)

test_us109: test_us109.c report_thread.o report_proc.o violation.o types.h
	$(CC) $(CFLAGS) -o test_us109 test_us109.c report_thread.o report_proc.o violation.o $(LDLIBS)

test_us110: test_us110.c types.h
	$(CC) $(CFLAGS) -o test_us110 test_us110.c $(LDLIBS)

test: test_us105 test_us106 test_us107 test_us108 test_us109 test_us110 test_scenarios
	@echo "--- Unit Tests ---"
	./test_us105
	@echo ""
	@echo "--- US106 Tests ---"
	./test_us106
	@echo ""
	@echo "--- US107 Unit Tests ---"
	./test_us107
	@echo ""
	@echo "--- US108 Unit Tests ---"
	./test_us108
	@echo ""
	@echo "--- US109 Unit Tests ---"
	./test_us109
	@echo ""
	@echo "--- US110 Unit Tests ---"
	./test_us110
	@echo ""
	@echo "--- Scenario Tests ---"
	./test_scenarios

clean:
	rm -f $(OBJS) flight_sim report.txt violations.log
	rm -f test_us105 test_us106 test_us107 test_us108 test_us109 test_us110 test_scenarios
	rm -f test_us109_report.txt test_us109_violations.log
	rm -rf *.dSYM

run-clean: flight_sim
	./flight_sim clean.json

run-violation: flight_sim
	./flight_sim violation.json

run-threshold: flight_sim
	./flight_sim threshold.json

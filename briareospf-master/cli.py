import subprocess

# Define the path to the tracer programs
ENTER_TRACER_PATH = "syscall_enter_tracer.py"
EXIT_TRACER_PATH = "syscall_exit_tracer.py"

# Prompt the user to enter which tracer program to run
tracer_choice = input("Enter 'enter' to run the enter tracer, or 'exit' to run the exit tracer: ")

# Prompt the user for the arguments to pass to the tracer program (if any)
pid_values = input("Enter comma-separated PIDs (optional): ")
syscall_values = input("Enter comma-separated system call numbers (optional): ")

# Determine which tracer program to run based on the user input
if tracer_choice == "enter":
    tracer_path = ENTER_TRACER_PATH
    args = []
    if pid_values:
        args += ["--pid", pid_values]
    if syscall_values:
        args += ["--syscall", syscall_values]
elif tracer_choice == "exit":
    tracer_path = EXIT_TRACER_PATH
    args = []
    if pid_values:
        args += ["--pid", pid_values]
    if syscall_values:
        args += ["--syscall", syscall_values]
else:
    print("Invalid choice.")
    exit()

# Run the tracer program with the specified arguments (if any)
subprocess.Popen(["python3", tracer_path] + args)
input("Press Enter to stop the tracer program\n")
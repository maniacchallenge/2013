package main

import (
	"bufio"
	"fmt"
	"master/betternode"
	"master/database"
	"master/log"
	"master/profile"
	"master/rounds"
	"master/server"
	"os"
	"runtime"
	"strings"
	"time"
)

func main() {
	defer func() {
		log.Close()             // Close log
		server.Close()          // Close server
		betternode.Close()      // Close nodes
		profile.StopProfile()   // Close profile
		time.Sleep(time.Second) // Wait for everything to close
	}()

	// Make Go run parallel
	runtime.GOMAXPROCS(runtime.NumCPU())

	log.Message("ManiacMaster starting.")

	// Initialize database
	log.Message("Connecting to database.")
	err := database.Connect("localhost:27018", "maniac", "", "")
	if err != nil {
		log.Error(err)
		fmt.Println("Error:", err.Error())
	}

	// Create TCP server
	log.Message("Starting TCP server.")
	err = server.Start(":6789", betternode.NewNode)
	if err != nil {
		log.Error(err)
		fmt.Println("Error:", err.Error())
	}

	// Take commands
	log.Message("Ready for command line input.")

	fmt.Println("MANIAC Master ready.")
	var command string

	s := bufio.NewScanner(os.Stdin)

quit:
	for {
		// Get next command
		fmt.Print("M> ")
		if !s.Scan() {
			fmt.Println("Input ended!")
			break
		}
		command = s.Text()

		// to trimmed lower case
		command = strings.ToLower(strings.TrimSpace(command))

		switch command {
		case "help": // show helpful text
			printHelp()

		case "quit", "exit": // quits the master
			break quit

		case "next": // starts new round
			rounds.NewRound()

		case "send": // sends new message
			rounds.NewTransaction()

		case "info": // show info about the master
			printInfo()

		case "load":
			fmt.Print("Profile file: ")
			fmt.Scanln(&command)
			profile.OpenProfile(command)

		case "run":
			fmt.Print("Profile name: ")
			fmt.Scanln(&command)
			go profile.RunProfile(command)

		case "kill":
			profile.StopProfile()

		case "list":
			profile.ListProfiles()

		case "close":
			fmt.Print("Profile name: ")
			fmt.Scanln(&command)
			profile.CloseProfile(command)

		default:
			fmt.Println("Invalid command: Type \"help\" to get help.")

		}
	}
}

func printHelp() {
	fmt.Println("Commands:\n" +
		"\thelp  - shows this help message\n" +
		"\tquit  - shuts down the master\n" +
		"\tnext  - starts a new round\n" +
		"\tsend  - generates and sends a new message\n" +
		"\tload  - Loads a new profile\n" +
		"\tlist  - Lists all loaded profiles\n" +
		"\trun   - Runs a profile\n" +
		"\tkill  - Kills a running profile\n" +
		"\tclose - Closes a loaded profile\n" +
		"\tinfo  - prints info about the current status of the master")
}

func printInfo() {
	gos := runtime.NumGoroutine()
	ths := runtime.GOMAXPROCS(0)
	cpu := runtime.NumCPU()
	nds := betternode.CountNodes()

	fmt.Printf("Go Routines:\t%d\n"+
		"Threads:\t%d\n"+
		"Cores:\t\t%d\n"+
		"Connected:\t%d\n", gos, ths, cpu, nds)
}

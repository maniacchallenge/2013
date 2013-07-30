package profile

import (
	"bufio"
	"fmt"
	"master/rounds"
	"os"
	"strconv"
	"strings"
	"time"
)

var (
	profiles map[string]*os.File
	kill     chan bool
)

func init() {
	profiles = make(map[string]*os.File)
	kill = make(chan bool, 1)
}

func StopProfile() {
	select {
	case kill <- true:
	default:
	}
}

func OpenProfile(filename string) {
	name := strings.SplitN(filename, ".", 2)[0]
	if name == "" {
		name = filename
	}

	file, err := os.Open(filename)
	if err != nil {
		fmt.Println("Unable to open profile:", err.Error())
		return
	}

	profiles[name] = file
}

func CloseProfile(name string) {
	_, ok := profiles[name]
	if !ok {
		fmt.Println("This profile does not exist.")
		return
	}

	profiles[name].Close()
	delete(profiles, name)
	fmt.Println("The profile has been unloaded:", name)
}

func ListProfiles() {
	if len(profiles) == 0 {
		fmt.Println("No profiles loaded.")
		return
	}

	for k, _ := range profiles {
		fmt.Println(k)
	}
}

func RunProfile(name string) {
	file, ok := profiles[name]
	if !ok {
		fmt.Println("There is no such profile loaded.")
		return
	}

	// Remove kill
	select {
	case <-kill:
	default:
	}

	r := bufio.NewReader(file)
	fmt.Println("Profile started.")

outer:
	for {
		// Check if we have been killed
		select {
		case <-kill:
			break outer
		default:
		}

		// Read a line from the file
		line, err := r.ReadString('\n')
		if err != nil {
			if err.Error() == "EOF" {
				break
			}

			fmt.Println("There has been an error while reading the profile.")
			return
		}

		// Get rid of the white spaces and comments
		line = strings.TrimSpace(line)
		if line == "" || line[0] == '#' {
			continue
		}

		// Figure out the command
		cmd := strings.Split(line, " ")
		switch cmd[0] {
		case "print": // We want to print some text
			fmt.Printf("(p %s):", name)
			for i := 1; i < len(cmd); i++ {
				fmt.Printf(" %s", cmd[i])
			}
			fmt.Println()

		case "sleep": // We want to sleep a couple of seconds
			if len(cmd) < 2 {
				fmt.Println("You must specify the number of seconds to sleep.")
				break
			}
			sec, err := strconv.ParseInt(cmd[1], 10, 64)
			if err != nil {
				fmt.Println("The sleep time must be a number.")
				break
			}
			time.Sleep(time.Duration(sec) * time.Second)

		case "next": // Initiate the next round
			rounds.NewRound()

		case "send": // Send a transaction
			rounds.NewTransaction()

		case "mode": // Set a sending mode
			if len(cmd) < 2 {
				fmt.Println("You must specify the mode.")
				break
			}
			tpermin, err := strconv.ParseInt(cmd[1], 10, 64)
			if err != nil {
				fmt.Println("Please specify the number of transactions per minute.")
				break
			}
			rounds.AutogenTransactions(tpermin)

		case "ceil":
			if len(cmd) < 2 {
				fmt.Println("You must specify the ceil.")
				break
			}
			ceil, err := strconv.ParseInt(cmd[1], 10, 64)
			if err != nil {
				fmt.Println("Please specify the ceil.")
				break
			}
			rounds.SetCeil(int(ceil))

		case "fine":
                        if len(cmd) < 2 {
                                fmt.Println("You must specify the fine.")
                                break
                        }
                        ceil, err := strconv.ParseInt(cmd[1], 10, 64)
                        if err != nil {
                                fmt.Println("Please specify the fine.")
                                break
                        }
                        rounds.SetFine(int(ceil))

		case "hops":
                        if len(cmd) < 2 {
                                fmt.Println("You must specify the hops.")
                                break
                        }
                        ceil, err := strconv.ParseInt(cmd[1], 10, 64)
                        if err != nil {
                                fmt.Println("Please specify the hops.")
                                break
                        }
                        rounds.SetHops(int(ceil))

		default: // Woops, we don't know this one
			fmt.Println("Unrecogized command:", cmd[0])
		}
	}

	fmt.Println("Profile stopped.")
	file.Seek(0, os.SEEK_SET)
}

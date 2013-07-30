package transaction

import (
	//	"fmt"
	"bufio"
	"bytes"
	"fmt"
	"master/betternode"
	"master/database"
	"master/log"
	"os"
	"time"
)

var (
	Add           chan *betternode.Transaction
	formerDevices []string

	jsMap1, jsReduce1, jsFinalize1 string
	jsMap2, jsReduce2, jsFinalize2 string

	balanceState map[string]int
)

func init() {
	Add = make(chan *betternode.Transaction, 5)
	go manager()

	jsMap1 = loadFile("map1.js")
	jsMap2 = loadFile("map2.js")
	jsReduce1 = loadFile("reduce1.js")
	jsReduce2 = loadFile("reduce2.js")
	jsFinalize1 = loadFile("finalize1.js")
	jsFinalize2 = loadFile("finalize2.js")

	balanceState = make(map[string]int)
}

///////////////////////////////////////////////////////////////////////////////

func manager() {
	startle := make(chan bool)
	//	update := time.Now()
	update := time.Date(1990, time.June, 30, 16, 58, 7, 0, time.Local)

	// Startle us every five seconds
	go periodic(startle)

	for {

		select {

		case t, ok := <-Add: // Added new transaction
			if !ok {
				return
			}
			go startler(startle, t.HopCount*3+1)

		case <-startle: // Notification, we should map-reduce
			updateDatabase(update)
			sendUpdates()

		}

	}

}

func startler(ch chan bool, secs int) {
	time.Sleep(time.Duration(secs) * time.Second)
	ch <- true
}

func periodic(ch chan bool) {
	for {
		time.Sleep(time.Second)
		ch <- true
	}
}

///////////////////////////////////////////////////////////////////////////////

func updateDatabase(lastUpdate time.Time) time.Time {
	// We log the current to so me are sure we do not miss data next time
	result := time.Now()

	// Update "transactions"
	database.MapReduce1(lastUpdate,
		jsMap1,
		jsReduce1,
		jsFinalize1,
	)

	// Create "balance"
	database.MapReduce2(lastUpdate,
		jsMap2,
		jsReduce2,
		jsFinalize2,
	)

	return result
}

///////////////////////////////////////////////////////////////////////////////

type Wrapper struct {
	Value *betternode.Balance `bson:"value"`
}

func sendUpdates() {
	// Query the database for the balances
	q := database.Find(nil)

	// Retrieve results
	var wubba []*Wrapper
	err := q.All(&wubba)
	if err != nil {
		log.Error(err)
	}

	result := make(map[string]*betternode.Balance)
	for _, w := range wubba {
		_, ok := balanceState[w.Value.Device]
		if ok {
			if balanceState[w.Value.Device] == len(w.Value.History) {
				continue
			}
		}
		balanceState[w.Value.Device] = len(w.Value.History)

		result[w.Value.Device] = w.Value
	}

	// Send results to the clients
	betternode.SendBalances(result)
}

///////////////////////////////////////////////////////////////////////////////

func loadFile(filename string) string {
	file, err := os.Open(filename)
	if err != nil {
		fmt.Println("Error: ", err.Error())
		return ""
	}
	defer file.Close()

	reader := bufio.NewReader(file)
	buf := bytes.NewBufferString("")

	for {
		line, err := reader.ReadString('\n')
		if err != nil {
			return buf.String()
		}

		buf.WriteString(line)
	}
}

package rounds

import (
	"fmt"
	"master/betternode"
	"master/database"
	"master/log"
	"master/transaction"
	"time"
)

///////////////////////////////////////////////////////////////////////////////

var (
	idgen   chan int
	RoundId string
	kill    chan bool
	ceil, hops, fine int
	mapping map[string]string
)

func init() {
	idgen = make(chan int)

	go func() {
		i := int(1)
		for {
			idgen <- i
			i++
		}
	}()
	ceil = 40
	hops = 10
	fine = 20

	// Map eth0 to wlan0
	mapping = make(map[string]string)

	for i := 0; i < 256; i++ {
		mapping[fmt.Sprintf("192.168.17.%d", i)] = fmt.Sprintf("172.16.17.%d", i)
	}
}

///////////////////////////////////////////////////////////////////////////////

func NewRound() {
	RoundId = fmt.Sprintf("maniac%s", time.Now().Format(time.RFC3339Nano))
	database.Inject(RoundId)
	database.Insert(map[string]string{"round": RoundId})
}

///////////////////////////////////////////////////////////////////////////////

func NewTransaction() {
	if betternode.CountNodes() == 0 {
		fmt.Println("Unable to initiate transaction: No nodes connected.")
		return
	}

	from := betternode.RandomNode()
	to := betternode.RandomNode()

	for i := 1; i < 1000; i++ {
		if to != from {
			break
		}
		to = betternode.RandomNode()
	}

	log.Message("NewTransmission()")
	t := betternode.NewTransaction(
		<-idgen,
		mapping[to],
		hops,
		"Lorem ipsum",
		fine,
		ceil,
		RoundId,
	)
	// Send transaction
	log.Message("NewTransmission() 1")
	betternode.SendTransaction(t, from)
	log.Message("NewTransmission() 2")
	database.Insert(t)
	log.Message("NewTransmission() 3")
	transaction.Add <- t
	log.Message("NewTransmission() 4")
}

func generator(dur time.Duration) {
	k := make(chan bool)
	kill = k

	for {
		time.Sleep(dur)
		select {
		case <-k:
			return
		default:
			NewTransaction()
		}
	}

}

func AutogenTransactions(tpermin int64) {
	StopAutogen()

	if tpermin < 0 {
		fmt.Println("The transactions per minute must not be below 0.")
		return
	}

	// Stop autogeneration
	if tpermin == 0 {
		return
	}

	ms := time.Duration(tpermin) * time.Millisecond
	go generator(ms)
}

func StopAutogen() {
	if kill != nil {
		close(kill)
		kill = nil
	}
}

func SetCeil(c int) {
	ceil = c
}

func SetFine(c int) {
	fine = c
}

func SetHops(c int) {
	hops = c
}


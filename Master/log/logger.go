package log

import (
	"fmt"
	"os"
	"time"
)

func init() {
	log, e := os.OpenFile("master.log", os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0666)
	if e != nil {
		panic(e)
	}

	chlog = make(chan string, 10)

	go logger(log, chlog)
}

var (
	log   *os.File
	chlog chan string
)

func logger(f *os.File, c chan string) {

	for x := range c {
		f.WriteString(fmt.Sprintf("%s - %s\n", time.Now().Format("2006-01-02 15:04:05"), x))
		f.Sync()
	}
	f.Close()

}

func Error(err error) {
	chlog <- err.Error()
}

func Panic(p interface{}) {
	chlog <- fmt.Sprint(p)
}

func Message(msg string) {
	chlog <- msg
}

func Close() {
	close(chlog)
}

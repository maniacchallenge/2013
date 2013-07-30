package main

import (
	"bytes"
	"fmt"
	"net/http"
	"os"
	"time"
)

func main() {
	fmt.Println("OLSR Logger")
	fmt.Println("Loading list of nodes...")

	host, _ := os.Hostname()

	file, err := os.OpenFile(host, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0666)
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	for {
		req := download("http://localhost:2006/all")
		file.WriteString(time.Now().Format(time.RFC3339Nano))
		file.WriteString("\n")
		file.WriteString(req)
		file.Sync()
		time.Sleep(5 * time.Second)
	}
}

func download(url string) string {
	fmt.Printf("downloading %s\n", url)

	resp, err := http.Get(url)
	if err != nil {
		return err.Error()
	}

	buf := &bytes.Buffer{}

	for {
		b := make([]byte, 0, 256)
		_, err := resp.Body.Read(b)
		if err != nil {
			fmt.Println(buf.String())
			return buf.String()
		}
		buf.Write(b)
	}
}

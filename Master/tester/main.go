package main

import (
	"bufio"
	"bytes"
	"fmt"
	"labix.org/v2/mgo"
	"labix.org/v2/mgo/bson"
	"os"
)

var p, b, t *mgo.Collection

func main() {
	var jsMap, jsReduce, jsFinalize string

	fmt.Println("Connecting to DB...")
	s, err := mgo.Dial("localhost:27018")
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	d := s.DB("maniac")

	p = d.C("packets")
	t = d.C("transactions")
	//b = d.C("balances")

	scan := bufio.NewScanner(os.Stdin)

	for {
		fmt.Print("Enter> ")
		scan.Scan()

		fmt.Println("Reading files...")
		jsMap = loadFile("map.js")
		jsReduce = loadFile("reduce.js")
		jsFinalize = loadFile("finalize.js")

		fmt.Println("Map Reduce...")
		mapReduce(jsMap, jsReduce, jsFinalize)
	}
}

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

func mapReduce(jsMap, jsReduce, jsFinalize string) {
	mr := &mgo.MapReduce{
		Map:      jsMap,
		Reduce:   jsReduce,
		Finalize: jsFinalize,
		Out: bson.M{
			"replace": "transactions",
		},
	}

	m, err := p.Find(nil).MapReduce(mr, nil)
	if err != nil {
		fmt.Println("Error:", err.Error())
	}
	fmt.Println(m)
}

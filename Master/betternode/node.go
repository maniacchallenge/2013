package betternode

import (
	"encoding/json"
	"master/database"
	"master/log"
	"math/rand"
	"net"
	"strings"
	"sync"
	"time"
)

///////////////////////////////////////////////////////////////////////////////

type Check struct {
	TransactionID int `json:"transactionID" bson:"transactionID"`
	Gain          int `json:"amount" bson:"amount"`
}

func NewCheck(transactionID int, gain int) *Check {
	return &Check{
		TransactionID: transactionID,
		Gain:          gain,
	}
}

///////////////////////////////////////////////////////////////////////////////

type Balance struct {
	Device  string   `json:"device" bson:"device"`
	Balance int      `json:"balance" bson:"balance"`
	History []*Check `json:"balanceUpdates" bson:"balanceUpdates"`
}

func NewBalance(device string, balance int, history []*Check) *Balance {
	if history == nil {
		history = make([]*Check, 0)
	}

	return &Balance{
		Device:  device,
		Balance: balance,
		History: history,
	}
}

func (b *Balance) AddCheck(check *Check) {
	b.History = append(b.History, check)
}

///////////////////////////////////////////////////////////////////////////////

type CheckUpdate struct {
	Type    string              `json:"type" bson:"type"`
	Devices map[string]*Balance `json:"devices" bson:"devices"`
}

func NewCheckUpdate(devices map[string]*Balance) *CheckUpdate {
	if devices == nil {
		devices = make(map[string]*Balance)
	}

	return &CheckUpdate{
		Type:    "U",
		Devices: devices,
	}
}

func (c *CheckUpdate) AddDevice(d *Balance) {
	c.Devices[d.Device] = d
}

///////////////////////////////////////////////////////////////////////////////

type Transaction struct {
	Type               string    `json:"type" bson:"type"`
	TransactionID      int       `json:"transactionID" bson:"transactionID"`
	FinalDestinationIP string    `json:"finalDestinationIP" bson:"finalDestinationIP"`
	HopCount           int       `json:"deadline" bson:"deadline"`
	Data               string    `json:"payload" bson:"payload"`
	Fine               int       `json:"fine" bson:"fine"`
	Ceil               int       `json:"ceil" bson:"ceil"`
	Time               time.Time `json:"-" bson:"time"`
	Round              string    `json:"-" bson:"round"`
}

func NewTransaction(transactionID int, finalDestinationIP string, hopCount int,
	data string, fine, ceil int, round string) *Transaction {
	return &Transaction{
		Type:               "X",
		TransactionID:      transactionID,
		FinalDestinationIP: finalDestinationIP,
		HopCount:           hopCount,
		Data:               data,
		Fine:               fine,
		Ceil:               ceil,
		Time:               time.Now(),
		Round:              round,
	}
}

///////////////////////////////////////////////////////////////////////////////

type Node struct {
	Conn         *net.TCPConn
	Clients      []string
	Cutex        sync.RWMutex
	Updates      chan map[string]*Balance
	Transactions chan *Transaction
}

func NewNode(conn *net.TCPConn) {
	// Create structure
	node := &Node{
		Conn:         conn,
		Clients:      make([]string, 0),
		Updates:      make(chan map[string]*Balance, 1),
		Transactions: make(chan *Transaction, 1),
	}

	// Insert into global nodes list
	nutex.Lock()
	nodes = append(nodes, node)
	nutex.Unlock()

	// Start sender and receiver
	go node.sender()
	go node.receiver()
}

// Removes a node from the list
func RemoveNode(n *Node) {
	nutex.Lock()

	for i, m := range nodes {
		if m == n {
			nodes = append(nodes[:i], nodes[i+1:]...)
		}
	}

	nutex.Unlock()
}

// Sends data to the node
func (n *Node) sender() {
	defer RemoveNode(n)

	enc := json.NewEncoder(n.Conn)

	for {
		log.Message("SenderLoop")
		select {

		case b, ok := <-n.Updates: // There is an balance update upcoming
			if !ok {
				n.Conn.Close()
				return
			}
			enc.Encode(NewCheckUpdate(b)) // Send as json

		case t, ok := <-n.Transactions: // We want to send a transaction
			if !ok {
				n.Conn.Close()
				return
			}
			enc.Encode(t) // Send the transaction

		}
	}

}

// Gets the data from a node and saves into the database
func (n *Node) receiver() {
	dec := json.NewDecoder(n.Conn)

	for {
		var obj interface{}
		err := dec.Decode(&obj)
		if err != nil {
			log.Error(err)
			if err.Error() == "EOF" {
				close(n.Transactions)
				return
			}
			// Hack: recover
			dec = json.NewDecoder(n.Conn)
		}

		log.Message("Received message from " + n.Conn.RemoteAddr().String() + ".")

		//		n.updateDevices(obj)
		database.Insert(obj)
	}
}

func (n *Node) updateDevices(obj interface{}) {
	// Is it an json object?
	m, ok := obj.(map[string]interface{})
	if !ok {
		return
	}

	// Has it a "type" member?
	p, ok := m["type"]
	if !ok {
		return
	}

	// Is that "type" member a string?
	t, ok := p.(string)
	if !ok {
		return
	}

	// Is "type" == "U"?
	if t != "U" {
		return
	}

	// Has it a member "devices"?
	q, ok := m["devices"]
	if !ok {
		return
	}

	// Is that member an array?
	d, ok := q.([]interface{})
	if !ok {
		return
	}

	// Convert array
	c := make([]string, len(d))
	for i, v := range d {
		c[i] = v.(string)
	}

	// Replace old clients list
	n.Cutex.Lock()
	n.Clients = c
	n.Cutex.Unlock()
}

func (n *Node) CountClients() int {
	n.Cutex.RLock()
	defer n.Cutex.RUnlock()

	return len(n.Clients)
}

///////////////////////////////////////////////////////////////////////////////

var (
	nodes  []*Node
	nutex  sync.RWMutex
	random *rand.Rand
)

func init() {
	// Random seed
	random = rand.New(rand.NewSource(time.Now().UnixNano()))
}

func SendBalances(balances map[string]*Balance) {
	log.Message("SendBalances()")

	nutex.RLock()

	for _, n := range nodes {
		n.Updates <- balances
	}

	nutex.RUnlock()
}

func SendTransaction(transaction *Transaction, to string) {
	log.Message("SendTransaction()")

	nutex.RLock()
	defer nutex.RUnlock()

	for _, n := range nodes {
		s := strings.SplitN(n.Conn.RemoteAddr().String(), ":", 2)[0]
		if s == to {
			n.Transactions <- transaction
			break
		}
	}
}

func CountNodes() int {
	log.Message("CountNodes()")

	nutex.RLock()
	defer nutex.RUnlock()

	return len(nodes)
}

func RandomNode() string {
	log.Message("RandomNode()")

	nutex.RLock()
	defer nutex.RUnlock()

	n := nodes[random.Intn(len(nodes))].Conn.RemoteAddr().String()
	log.Message(n)
	s := strings.SplitN(n, ":", 2)[0]
	log.Message(s)

	return s
}

func GetDevices() []string {
	log.Message("GetDevices()")

	result := make([]string, 0, 0)

	nutex.RLock()

	for _, n := range nodes {
		result = append(result, n.Clients...)
	}

	nutex.RUnlock()

	return result
}

func Close() {
	nutex.Lock()

	for _, s := range nodes {
		s.Conn.Close()
	}

	nutex.Unlock()
}

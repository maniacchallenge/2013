package database

import (
	"labix.org/v2/mgo"
	"labix.org/v2/mgo/bson"
	"master/log"
	"time"
)

var (
	inject                string
	sess                  *mgo.Session
	packets, transactions *mgo.Collection
	balances, devices     *mgo.Collection
)

func init() {
	inject = ""
}

func Connect(servers, database, user, password string) error {
	// Connect to MongoDB(s)
	s, err := mgo.Dial(servers)
	if err != nil {
		return err
	}
	d := s.DB(database)

	// Authenticate if necessary
	if user != "" {
		err = d.Login(user, password)
		if err != nil {
			return err
		}
	}

	// Create collections if they don't exist.
	packets = d.C("packets")
	transactions = d.C("transactions")
	balances = d.C("balances")

	// Save session
	sess = s

	// Create channel and start runner
	return nil
}

func Close() {
	sess.Close()
}

func Insert(i interface{}) {
	// If it is a map, we add an timestamp
	m, ok := i.(map[string]interface{})
	if ok {
		// Insert time stamp
		m["time"] = time.Now()

		// Insert round id
		if inject != "" {
			m["round"] = inject
		}
		packets.Insert(m)
	} else {
		packets.Insert(i)
	}
}

func Inject(inj string) {
	inject = inj
}

func Find(query interface{}) *mgo.Query {
	return balances.Find(query)
}

func MapReduce1(from time.Time, jsMap, jsReduce, jsFinalize string) *mgo.MapReduceInfo {
	mr := &mgo.MapReduce{
		Map:      jsMap,
		Reduce:   jsReduce,
		Finalize: jsFinalize,
		Out:      bson.M{"replace": "transactions"},
	}

	q := packets.Find(bson.M{"type": bson.M{"$exists": true}})

	info, err := q.MapReduce(mr, nil)
	if err != nil {
		log.Error(err)
		return nil
	}

	return info
}

func MapReduce2(from time.Time, jsMap, jsReduce, jsFinalize string) *mgo.MapReduceInfo {
	mr := &mgo.MapReduce{
		Map:      jsMap,
		Reduce:   jsReduce,
		Finalize: jsFinalize,
		Out:      bson.M{"replace": "balances"},
	}

	q := transactions.Find(nil)

	info, err := q.MapReduce(mr, nil)
	if err != nil {
		log.Error(err)
		return nil
	}

	return info
}

func InsertDevices(devs []string) {
	for _, d := range devs {
		devices.Insert(bson.M{"device": d})
	}
}

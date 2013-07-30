package server

import (
	"net"
)

type TCPServe func(*net.TCPConn)

var (
	s *net.TCPListener
)

func Start(address string, serve TCPServe) error {
	// Resolve address
	addr, err := net.ResolveTCPAddr("tcp", address)
	if err != nil {
		return err
	}

	// Create socket
	sock, err := net.ListenTCP("tcp", addr)
	if err != nil {
		return err
	}

	// Save socket
	s = sock

	// Start listener
	go listener(sock, serve)

	return nil
}

func Close() {
	s.Close()
}

func listener(sock *net.TCPListener, serve TCPServe) {
	for {
		conn, err := sock.AcceptTCP()
		if err != nil {
			return
		}
		go serve(conn)
	}
}

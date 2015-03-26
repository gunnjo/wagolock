package com.josephgunn

import akka.actor.{Actor, ActorRef}
import akka.actor.ActorSystem
import akka.actor.Props
import akka.event.Logging
import akka.actor.ReceiveTimeout

import scala.concurrent.duration._

import java.net._
import java.io._

import net.wimpi.modbus.net._
import net.wimpi.modbus.msg._
import net.wimpi.modbus.io._
import net.wimpi.modbus.util.BitVector

case class Unlock( lock: Int)

case class Status( lock: Int)

case class LockerStatus( open: Boolean)

case class LockerStati(c: Array[Boolean])


object LockerServer {
  	def props(transactor: ActorRef, count: Int, ipaddr: String, port: Int): Props = {
  		Props(new LockerServer( transactor, count, ipaddr, port))
  }
}

class LockerServer(messageClient: ActorRef, count: Int, ipaddr: String, port: Int) extends Actor {
implicit def BitVector2BoolList(b: BitVector): Array[Boolean] = {
	val cl: Array[Byte] = b.getBytes
	for ( c <- cl) yield ( c=='0')
} 
	var lockerOpen = List[Boolean]()

	import context.system

	context.setReceiveTimeout(15 seconds) 
	/* Variables for storing the parameters */
	val addr:InetAddress = InetAddress.getByName(ipaddr)

	val ref = 0

	val con:  TCPMasterConnection = new TCPMasterConnection(addr)
	con.setPort(port)
	con.connect()

	val req: ReadInputDiscretesRequest = new ReadInputDiscretesRequest(ref, count)

	val trans: ModbusTCPTransaction = new ModbusTCPTransaction(con)
	trans.setRequest(req)


	def receive = {

		case Unlock(locker) =>
		case Status(locker) =>
			messageClient ! LockerStatus(lockerOpen(locker))
		case "close" =>
			con.close()
		case ReceiveTimeout => {
			trans.execute()
			val res: ReadInputDiscretesResponse = trans.getResponse().asInstanceOf[ReadInputDiscretesResponse]
			println("Digital Inputs Status=" + res.getDiscretes().toString())
			messageClient ! LockerStati(res.getDiscretes())
		}
		case msg @ _ => 
			println("LockerServer unknown message " + msg)

	}
}

class StatusClient() extends Actor {
 	val lockerActor = context.actorOf( LockerServer.props(self, 8, "192.168.34.123", 502), name = "locks")
	lockerActor !Unlock(2)
	def receive = {
		case msg @ LockerStati(m) => {
			m.zipWithIndex foreach { case (l, i) => println(s"locker $i is $l")}
		} 
		case msg @ _ => 
			println("LockerServer unknown message " + msg)

	}
}
object Wagolock extends App {
	val system = ActorSystem("Wago")
  val transactor = system.actorOf( Props(new StatusClient), name = "statusClient")


	system.awaitTermination()

}

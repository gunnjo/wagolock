package com.josephgunn

import akka.actor.{Actor, ActorRef, PoisonPill}
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

case class LockerStati(c: IndexedSeq[Boolean])


object LockerServer {
  	def props(transactor: ActorRef, count: Int, ipaddr: String, port: Int): Props = {
  		Props(new LockerServer( transactor, count, ipaddr, port))
  }
}

class LockerServer(messageClient: ActorRef, count: Int, ipaddr: String, port: Int) extends Actor {
implicit def BitVector2Bool(b: BitVector, i: Integer): Boolean = {
	val s = b.toString
	s(i) match { case '0' => true; case _ => false }
} 
implicit def BitVector2BoolList(b: BitVector): IndexedSeq[Boolean] = {
	b.toString().map { case '0' => true; case _ => false }
} 
	var lockerOpen = new BitVector(count)

	import context.system

	context.setReceiveTimeout(15 seconds) 
	/* Variables for storing the parameters */
	val addr:InetAddress = InetAddress.getByName(ipaddr)

	val readRef = 0
	val writeRef = 0x200

	val con:  TCPMasterConnection = new TCPMasterConnection(addr)
	con.setPort(port)
	con.connect()

	val readReq: ReadInputDiscretesRequest = new ReadInputDiscretesRequest(readRef, count)
	val writeReq: WriteMultipleCoilsRequest = new WriteMultipleCoilsRequest(writeRef, count)

	val trans: ModbusTCPTransaction = new ModbusTCPTransaction(con)
	trans.setRequest(readReq)

	trans.execute()
	val res: ReadInputDiscretesResponse = trans.getResponse().asInstanceOf[ReadInputDiscretesResponse]
	lockerOpen = res.getDiscretes()			

	def receive = {

		case Unlock(locker) =>
			lockerOpen(locker) match {
				case false =>
					lockerOpen.setBit(locker, true)
					writeReq.setCoils(lockerOpen)
					trans.setRequest(writeReq)
					trans.execute()
					val res: WriteMultipleCoilsResponse = trans.getResponse().asInstanceOf[WriteMultipleCoilsResponse]
				case true =>
					println(s"locker $locker is Open")
			}
		case Status(locker) =>
			messageClient ! LockerStatus(lockerOpen(locker))
		case "close" =>
			con.close()
		case ReceiveTimeout => {
			trans.setRequest(readReq)
			trans.execute()
			val res: ReadInputDiscretesResponse = trans.getResponse().asInstanceOf[ReadInputDiscretesResponse]
			lockerOpen = res.getDiscretes()
			messageClient ! LockerStati(lockerOpen)
		}
		case msg @ _ => 
			println("LockerServer unknown message " + msg)

	}
	override def postStop() = con.close()

}

class StatusClient() extends Actor {
 	val lockerActor = context.actorOf( LockerServer.props(self, 8, "192.168.34.123", 502), name = "locks")
	lockerActor !Unlock(2)
	def receive = {
		case Unlock(b) => {
			lockerActor ! Unlock(b)
		} 
		case LockerStati(m) => {
			m.zipWithIndex foreach { case (l, i) => println(s"locker $i is $l")}
		} 
		case msg @ _ => 
			println("LockerServer unknown message " + msg)

	}
	override def postStop() = lockerActor !PoisonPill
}
object Wagolock extends App {

import scala.io._
	val system = ActorSystem("Wago")
  val transactor = system.actorOf( Props(new StatusClient), name = "statusClient")
  var working = true

  try {
  	while (true) {
		scala.io.StdIn.readInt match {
			case  i @ _ =>
				transactor ! Unlock(i)
		}

	}
	} catch {
		     case e: Exception => 
		     println(e)
	}
	transactor ! PoisonPill
}

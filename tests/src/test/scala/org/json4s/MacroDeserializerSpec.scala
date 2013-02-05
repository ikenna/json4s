package org.json4s

import org.specs2.mutable.Specification
import java.util.Date
import org.json4s.ParserUtil.ParseException
import Macros._

case class Junk(in1:Int, in2:String)
case class MutableJunk(var in1:Int,var in2:String)
case class MutableJunkWithField(var in1:Int) {
  var in2:String = ""
}
case class ThingWithJunk(name:String, junk:Junk)
case class Crazy(name:String,thg:ThingWithJunk)
case class WithOption(in:Int,opt:Option[String])
case class OptionOption(in:Option[Option[Int]])
case class JunkWithDefault(in1:Int, in2:String="Default...")
case class WithList(name:String, lst:List[Int])
case class WithObjList(name:String, list:List[ThingWithJunk])
case class Curried(in1:Int,in2:Int)(in3:Int)
case class WithTpeParams[U](in1:U)
class WithNstedTpeParams[U,U2](val in1: U, val in2:WithTpeParams[U2]) {
  override def equals(in:Any) = in match {
    case in:WithNstedTpeParams[U,U2] => in.in1 == in.in1 && in.in2 == in2
	case _ => false
  }
}
case class ResolvedParams[U](in3: U, in4:WithTpeParams[Int]) extends WithNstedTpeParams[U,Int](in3,in4)

case class Bill(in:Int)

class Billy[U](in:U)
case class BillyB(in:Int) extends Billy[Int](in)

case class WithDate(name:String, date: Date) {
  override def equals(in:Any) = in match {
    case that: WithDate => name == that.name && date.toString == that.date.toString
	case _ => false
  }
}

class ClassWithDef(val in:Int=4) {
  override def toString = s"ClassWithDef(in:$in)"
  override def equals(obj:Any) = obj match {
	  case a:ClassWithDef => a.in == in
	  case _ => false
  }
}

class Stopwatch {

  private var startTime = -1L
  private var stopTime = -1L
  private var running = false

  def start(): Stopwatch = {
    startTime = System.currentTimeMillis()
    running = true
    this
  }

  def stop(): Stopwatch = {
    stopTime = System.currentTimeMillis()
    running = false
    this 
  }

  def isRunning(): Boolean = running

  def getElapsedTime() = {
    if (startTime == -1) {
      0
    }
    if (running) {
      System.currentTimeMillis() - startTime
    }
    else {
      stopTime - startTime
    }
  }

  def reset() {
    startTime = -1
    stopTime = -1
    running = false
  }
}

case class ObjWithDefJunk(name:String, junk:Junk=Junk(-1,"Default"))

class MacroDeserializerSpec extends Specification {
  implicit val defaultFormats = DefaultFormats
  val refJunk = Junk(2,"cats")
  val refJunkDict = Map("d.in1"->refJunk.in1.toString,"d.in2"->refJunk.in2)

  "Macros.deserialize" should {
  
  "Build maps of primatives with string key" in {
    val data = Map("dd.a"->"1","dd.b" ->"2", "dd.c" -> "3")
    val expected = data.map { case (k,v) => (k.split("""\.""")(1),v.toInt) }
    
    deserialize[Map[String,Int]](data,"dd") must_== expected
  }
  
  "Build maps of primatives with Int key" in {
    val data = Map("dd.1"->"1","dd.2" ->"2", "dd.100" -> "3")
    val expected = data.map { case (k,v) => (k.split("""\.""")(1).toInt,v.toInt) }
    
    deserialize[Map[Int,Int]](data,"dd") must_== expected
  }
  
  "Build maps of Junks with string key" in {
    val data = Map("dd.a.in1"->"1","dd.a.in2"->"aaa","dd.b.in1" ->"2","dd.b.in2"->"bbb", "dd.c.in1" -> "3","dd.c.in2"->"ccc")
    val expected = Map("a"->Junk(1,"aaa"),"b"->Junk(2,"bbb"),"c"->Junk(3,"ccc"))
    
    deserialize[Map[String,Junk]](data,"dd") must_== expected
  }
  
  "Build a map with polymorphic types" in {
    //case class WithTpeParams[U](in1:U)
    val data = Map("dd.a.in1"->"2","dd.b.in1"->"3","dd.d.in1"->"4")
    val expected = Map("a"->WithTpeParams(2),"b"->WithTpeParams(3),"d"->WithTpeParams(4))
    deserialize[Map[String,WithTpeParams[Int]]](data,"dd") must_== expected
  }
  
  "Make a lot of things" in {
    //Junk(in1:Int, in2:String)
    //ThingWithJunk(name:String, junk:Junk)
    val stuff = new scala.collection.mutable.MutableList[ThingWithJunk]()
    val params = new scala.collection.mutable.HashMap[String,Any]
    import scala.util.Random
    val numObjs = 6
    (0 until numObjs) foreach{ i =>
      val thng = ThingWithJunk("name_"+i,Junk(Random.nextInt,"junker"+Random.nextInt))
      stuff += thng
      val str = "dd["+i.toString + "]"
      params+= str+".name" -> thng.name
      params+= str+".junk.in1" -> thng.junk.in1.toString
      params+= str+".junk.in2" -> thng.junk.in2
    }
    val mapParams = params.toMap
    val stopwatch = new Stopwatch
    var result = deserialize[List[ThingWithJunk]](mapParams,"dd")
    stopwatch.start
    result = deserialize[List[ThingWithJunk]](mapParams,"dd")
    stopwatch.stop
    println(s"--------------- Time to deserialize $numObjs objects: ${stopwatch.getElapsedTime} millisec ------------------")
    result must_== stuff.toList
  }
  
	"Primative Int" in {
	  val expected:Int = 5
	  val params = Map("d"->"5")
	  deserialize[Int](params,"d") must_== expected
	}
  
  "Primative Long" in {
	  val expected:Long = 5
	  val params =Map("d"->"5")
	  deserialize[Long](params,"d") must_== expected
	}
  
	"Generate a Junk" in {
	  deserialize[Junk](refJunkDict,"d") must_== refJunk
	}
	
	"Generate a MutableJunk" in {
	  deserialize[MutableJunk](refJunkDict,"d") must_== MutableJunk(2,"cats")
	}
  
  "Generate a MutableJunkWithField when field provided" in {
    //case class MutableJunkWithField(var in1:Int) {
    //  var in2:String = ""
    //}
    val expected = MutableJunkWithField(2)
    expected.in2 = "cats"
    deserialize[MutableJunkWithField](refJunkDict,"d") must_== expected
    
  }
  
  "Generate a MutableJunkWithField when field missing" in {
    //case class MutableJunkWithField(var in1:Int) {
    //  var in2:String = ""
    //}
    val expected = MutableJunkWithField(2)
    val params = Map("d.in1"->refJunk.in1.toString)
    deserialize[MutableJunkWithField](params,"d") must_== expected
    
  }
  
	"Generate a ThingWithJunk" in {
	  val expected = ThingWithJunk("Bob",Junk(2,"SomeJunk..."))
	  val stuff = Map("dog.junk.name"->expected.name,"dog.name"->expected.name,"dog.junk.in1"->expected.junk.in1.toString,
					  "dog.junk.in2"->expected.junk.in2)
					  
	  val result = deserialize[ThingWithJunk](stuff,"dog")
	
	  result must_== expected
	}
	
	"Generate a 3 fold deap case class" in {
	  val expected = Crazy("crazyBob...",ThingWithJunk("Bob",Junk(2,"SomeJunk...")))
	  val stuff = Map("d.name"->expected.name,"d.thg.name"->expected.thg.name,"d.thg.junk.in1"->expected.thg.junk.in1.toString,
					  "d.thg.junk.in2"->expected.thg.junk.in2)
					  
	  val result = deserialize[Crazy](stuff,"d")
	  result must_== expected
	}
  
	"Parse date info" in {
	  val expected = WithDate("Bob",new Date)
	  val params = Map("d.name"->"Bob","d.date"->defaultFormats.dateFormat.format(expected.date))
	  
	  deserialize[WithDate](params,"d") must_== expected
	}
  
	"Created ClassWithDef with param" in {
	  deserialize[ClassWithDef](Map("d.in"->"1"),"d") must_== (new ClassWithDef(1))
	}
	
	"Created ClassWithDef without param" in {
    val params = Map(""->"")
	  deserialize[ClassWithDef](params,"d") must_== (new ClassWithDef)
	}
	
	"Generate a JunkWithDefault with a value" in {
	  var expected = JunkWithDefault(refJunk.in1,refJunk.in2)
	  deserialize[JunkWithDefault](refJunkDict,"d") must_== expected
	}
	
	"Generate a JunkWithDefault without a value" in {
	  var expected = JunkWithDefault(refJunk.in1)
    val params = Map("d.in1"->"2")
	  deserialize[JunkWithDefault](params,"d") must_== expected
	}
	
	"Created ObjWithDefJunk without junk" in {
	  val expected = ObjWithDefJunk("Name")
	  val params = Map("d.name"->"Name")
	  deserialize[ObjWithDefJunk](params,"d") must_== expected
	}
	
	"Created ObjWithDefJunk with provided junk" in {
	  val expected = ObjWithDefJunk("Name",Junk(2,"Provided"))
	  val map = Map("d.name"->"Name","d.junk.in1"->"2","d.junk.in2"->"Provided")
	  deserialize[ObjWithDefJunk](map,"d") must_== expected
	}
	
	"Generate a recursive Option" in {
	  val expected = OptionOption(Some(Some(5)))
	  val stuff = Map("d.in"->"5")
	  
	  val result = deserialize[OptionOption](stuff,"d")
	  
	  result must_== expected
	}
	
	"Instance a case class with an Option" in {
	  val expected = WithOption(2,Some("Pizza pockets forever!"))
	  val params = Map("d.in"->"2","d.opt"->"Pizza pockets forever!")
	  deserialize[WithOption](params,"d") must_== expected
	}
	
	"Instance a case class with a missing Option" in {
	  val expected = WithOption(2,None)
	  val params = Map("d.in"->"2")
	  deserialize[WithOption](params,"d") must_== expected
	}
  "Handle type parameters" in {
	  //case class WithTpeParams[U](in1:U)
	  val expected = WithTpeParams(100)
	  val params = Map("d.in1"->"100")
	  deserialize[WithTpeParams[Int]](params,"d") must_== expected
	}
	
	"Handle a tuple" in {
	  val expected = (2,3,"cats")
	  val params = Map("d._1"->"2","d._2"->"3","d._3"->"cats")
	  
	  deserialize[Tuple3[Int,Int,String]](params,"d") must_== expected
	}
	
	"Handle nested type parameters, WithNstedTpeParams[U,U2](U, WithTpeParams[U2])" in {
	  val expected = new WithNstedTpeParams("cat",WithTpeParams(100))
	  val params = Map("d.in1"->"cat","d.in2.in1"->"100")
	  deserialize[WithNstedTpeParams[String,Int]](params,"d") must_== expected
	}
	
	"Handle partially resolved, ResolvedParams[U](in3: U, in4:WithTpeParams[Int])" in {
	  val expected = new ResolvedParams("cat",WithTpeParams(100))
	  val params = Map("d.in3"->"cat","d.in4.in1"->"100")
	  deserialize[ResolvedParams[String]](params,"d") must_== expected
	}
	
	"Curried case class" in {
	  val expected = Curried(1,2)(3)
	  val params = Map("d.in1"->"1","d.in2"->"2","d.in3"->"3")
	  deserialize[Curried](params,"d") must_== expected
	}
	
		// This fails to compile: complains about type parameters for List
	"parse List[Int]" in {
      val expected = 1::2::3::4::Nil
      val params = Map("d[0]"->"1","d[1]"->"2","d[2]"->"3","d[3]"->"4")
	  
	  val result = deserialize[List[Int]](params,"d")
	   
	  result must_== expected
  }
	
	"parse List[WithTpeParams[String]]" in {
      val expected = WithTpeParams("one")::WithTpeParams("two")::Nil
      val params = Map("d[0].in1"->"one","d[1].in1"->"two")
	  
	  val result = deserialize[List[WithTpeParams[String]]](params,"d")
	   
	  result must_== expected
  }
	
	"parse empty List[Int]" in {
      val expected:List[Int] = Nil
      val params:Map[String,Any] = Map()
	  val result = deserialize[List[Int]](params,"d")
	   
	  result must_== expected
  }
  
	"parse List[List[Int]]" in {
      val expected = (1::2::Nil)::(3::4::Nil)::Nil
      val params = Map("d[0][0]"->"1","d[0][1]"->"2","d[1][0]"->"3","d[1][1]"->"4")
	  
	  val result = deserialize[List[List[Int]]](params,"d")
	   
	  result must_== expected
  }
	 
	"Parse WithList" in {
	  val expected = WithList("Bob", 1::4::Nil)
	  val params = Map("d.name"->"Bob","d.lst[0]"->"1","d.lst[1]"->"4")
	  deserialize[WithList](params,"d") must_== expected
	}
	
	"parse WithObjList" in {
	//case class ThingWithJunk(name:String, junk:Junk)
      val expected = WithObjList("Bob",ThingWithJunk("Bobby",Junk(1,"one"))::ThingWithJunk("Bill",Junk(2,"two"))::Nil)
      val params = Map("d.name"->"Bob","d.list[1].name"->"Bill","d.list[0].name"->"Bobby","d.list[0].junk.in1"->"1","d.list[0].junk.in2"->"one","d.list[1].junk.in1"->"2","d.list[1].junk.in2"->"two")
      deserialize[WithObjList](params,"d") must_== expected
    }
	 
	"parse List[Bill]" in {
	//case class ThingWithJunk(name:String, junk:Junk)
      val expected = Bill(1)::Bill(3)::Nil
      val params = Map("d[0].in"->"1","d[1].in"->"3")
      deserialize[List[Bill]](params,"d") must_== expected
    }
	
	"parse BillyB which extends Billy[Int]" in {
	  val expected = BillyB(3)
	  val params = Map("d.in"->"3")
	  deserialize[BillyB](params,"d") must_== expected
	}
	
	"Throw ParseException with a bad map value for 'in'" in {
    val params = Map("d.in1"->"2ffds","d.in2"->"cats")
	  deserialize[Junk](params,"d") must throwA[ParseException](message="Error parsing value 'in1' to Int")
	}
	
  }
}
package strober

import firrtl._
import scala.collection.mutable.{ArrayBuffer, HashMap, LinkedHashMap, HashSet}
import scala.util.DynamicVariable

private class TransformContext {
  val shims = HashSet[String]()
  val wrappers = HashSet[String]()
  val modules = HashSet[String]()
  val children = HashMap[String, Vector[String]]()
}

class StroberTransforms extends Transform with SimpleRun {
  val passSeq = Seq(
    strober.passes.Analyses,
    strober.passes.Fame1Transform
  )
  def execute(circuit: Circuit, annotations: Seq[CircuitAnnotation]) = 
    run(circuit, passSeq)
}

class StroberCompiler extends Compiler {
  def transforms(writer: java.io.Writer): Seq[Transform] = Seq(
    new Chisel3ToHighFirrtl,
    new IRToWorkingIR,
    new ResolveAndCheck,
    new HighFirrtlToMiddleFirrtl,
    new StroberTransforms,
    new EmitFirrtl(writer)
  )
}

object StroberCompiler {
  private val contextVar = new DynamicVariable[Option[TransformContext]](None)
  private[strober] def context = contextVar.value.getOrElse (new TransformContext)

  private def moduleToFirrtl(c: Chisel.internal.firrtl.Circuit, dir: java.io.File) = {
    val firrtl = s"${dir}/${c.name}.fir"
    Chisel.Driver.dumpFirrtl(c, Some(new java.io.File(firrtl)))
    firrtl
  }

  private def transform(dutName: String, highFirrtl: String, dir: java.io.File) = {
    val firrtl = s"${dir}/${dutName}-strober.fir"
    Driver.compile(highFirrtl, firrtl, new StroberCompiler)
    firrtl
  }

  private def firrtlToVerilog(dutName: String, firrtl: String, dir: java.io.File) = {
    val v = s"${dir}/${dutName}.v"
    Driver.compile(firrtl, v, new VerilogCompiler)
    v
  }

  private[strober] def init[T <: Chisel.Module](w: SimWrapper[T]) {
    context.wrappers += w.name
  }

  def apply[T <: Chisel.Module](args: Array[String], w: => T) = {
    contextVar.withValue(Some(new TransformContext)) {
      Chisel.Driver.parseArgs(args)
      val circuit = Chisel.Driver.elaborate(() => w)
      val dir = new java.io.File(Chisel.Driver.targetDir)
      dir.mkdirs
      val highFirrtl = moduleToFirrtl(circuit, dir)
      val firrtl = transform(circuit.name, highFirrtl, dir)
      firrtlToVerilog(circuit.name, firrtl, dir)
      circuit
    }
  }
}

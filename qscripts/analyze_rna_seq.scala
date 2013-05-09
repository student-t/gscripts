package org.broadinstitute.sting.queue.qscripts

import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.queue.util.QScriptUtils
import net.sf.samtools.SAMFileHeader.SortOrder
import org.broadinstitute.sting.utils.exceptions.UserException
import org.broadinstitute.sting.commandline.Hidden
import org.broadinstitute.sting.queue.extensions.picard.{ReorderSam, SortSam, AddOrReplaceReadGroups, MarkDuplicates}
import org.broadinstitute.sting.queue.extensions.gatk._


class AnalizeCLIPSeq extends QScript {
  // Script argunment
  @Input(doc="input file")
  var input: List[File] = Nil

  @Argument(doc="species (hg19...)")
  var species: String =  _

  @Argument(doc="location of chr_sizes")
  var chr_sizes: String = _
 
  @Argument(doc="adapter to trim")
  var adapter: List[String] = Nil

  @Argument(doc="location to place trackhub (must have the rest of the track hub made before starting script)")
  var location: String = _

 class FilterRepetativeRegions(@Input inFastq: File, @Output outCounts: File, @Output outNoRep: File) extends CommandLineFunction {

	def commandLine = "bowtie -S -q -p 4 -e 100 -l 20 --un %s all_ref %s | grep -v \"@\" | perl /nas3/yeolab/Software/pipeflower/count_aligned_from_sam.pl > %s".format(outNoRep, inFastq, outCounts)

 }

 class FastQC(@Input inFastq: File) extends CommandLineFunction {

	def commandLine = "fastqc %s".format(inFastq)

 }

 class Cutadapt(@Input inFastq: File, @Output outFastq: File, @Output report: File, @Argument anywhere: List[String] = Nil, front: List[String] = Nil, @Argument overlap: Option[Int] = None, error_rate: Option[Double] = None, length: Option[Int] = None, quality_cutoff: Option[Int] = None) extends CommandLineFunction {
	//see argunments on cutadapt command line for more documentation
	def commandLine = "cutadapt -f fastq --match-read-wildcards --times 2" + optional("-e", error_rate) + optional("-O", overlap) + optional("--quality-cutoff", quality_cutoff) + optional("-m", length) + repeat("-b", anywhere) + repeat("-f", front) + required("-o", outFastq) + required(inFastq) + " > " +  report
 }

 class MapWithSTAR(@Input inFastq: File, @Output samFile: File, @Argument genome: String) extends CommandLineFunction{

	def commandLine = "/nas3/yeolab/Software/STAR/STAR_2.3.0e/STAR --runMode alignReads --runThreadN 4 --genomeDir /nas3/yeolab/Software/STAR/genomes/2.2.0/%s --genomeLoad LoadAndRemove --readFilesIn %s --outSAMunmapped Within --outFilterMultimapNmax 1 --outFileNamePrefix %s --outStd SAM > %s".format(genome, inFastq, samFile, samFile)

 }

 class sortBam extends CommandLineFunction {
	@Input(doc="Bam file to sort") 
	var inBam: File = _

	@Output(doc="Sorted bam file", required=false)
	var outBam: File = _

	def commandLine = "samtools sort %s %s".format(inBam, outBam)	

 }

  class genomeCoverageBed(@Input inBam: File, @Argument genomeSize: String, @Output bedGraph: File, @Argument strand: String = null) extends CommandLineFunction{
	//When it comes time to refactor use the @Input(doc, requiered=False) pattern...
	def commandLine = "genomeCoverageBed " + optional("-strand", strand) + required("-split") + required("-bg") + required("-ibam", inBam) + required("-g", genomeSize) +  " > " + bedGraph

 }
 
  case class sortSam (inSam: File, outBam: File, sortOrderP: SortOrder) extends SortSam {
    this.input :+= inSam
    this.output = outBam
    this.sortOrder = sortOrderP

  }

  case class markDuplicates(inBam: File, outBam: File, metrics_file: File, remove_duplicates: Boolean) extends MarkDuplicates {
	this.input = List(inBam)
	this.output = outBam
	this.metrics = metrics_file
	this.REMOVE_DUPLICATES = remove_duplicates
	this.isIntermediate = false
 } 
 class bedGraphToBigWig(@Input inBedGraph: File, @Argument genomeSize: String, @Output bigWig: File) extends CommandLineFunction{

	def commandLine = "bedGraphToBigWig %s %s %s".format(inBedGraph, genomeSize, bigWig)

 }

 class Clipper(@Input inBam: File, @Argument species: String, @Output outBed: File) extends CommandLineFunction{

 	def commandLine = "clipper -b %s -s %s -o %s".format(inBam, species, outBed) 

 }

 class Clip_Analysis(@Input inBam: File, @Input inBed: File, @Argument species: String) extends CommandLineFunction {
	
	def commandLine = "python /nas3/gpratt/clipper/clipper/src/CLIP_analysis.py --clusters %s -s %s --bam %s --regions_location /nas3/lovci/projects/ucscBED/%s --AS_Structure /nas3/yeolab/Genome/ensembl/AS_STRUCTURE/%sdata4 --genome_location /nas3/yeolab/Genome/ucsc/%s/chromosomes/all.fa --motif AAAAA --nrand 3 --rePhast".format(inBed, species, inBam, species, species, species) 

 }

 class MakeTrackHub(@Input bwFiles: List[File], @Argument location: String) extends CommandLineFunction {
	//@Input(doc="Bam file to sort") 
	//var bwFiles: List[File] = Nil 
	
	//@Argument(doc="Location") 
	//var location: String = _
	
	def commandLine = "python /nas3/gpratt/gscripts/make_trackhubs.py" + repeat(bwFiles) + required("--location", location)

 	
 }

  def script() {

//    val fileList: Seq[File] = QScriptUtils.createSeqFromFile(input)
     var bwFiles : List[File] = List()
   
    for (fastq_file: File <- input) {
    
    val noPolyAFastq  = swapExt(fastq_file, ".fastq", ".polyATrim.fastq")    
    val noPolyAReport = swapExt(noPolyAFastq, ".fastq", ".report")

    val noAdapterFastq = swapExt(noPolyAFastq, ".fastq", ".adapterTrim.fastq")
    val adapterReport = swapExt(noAdapterFastq, ".fastq", ".report")

    val filteredFastq = swapExt(noAdapterFastq, ".fastq", ".rmRep.fastq")    
    val filterd_results = swapExt(filteredFastq, ".fastq", ".counts")

    val samFile = swapExt(filteredFastq, ".fastq", ".sam")
    val sortedBamFile = swapExt(samFile, ".sam", ".sorted.bam")
    val rmDupedBamFile = swapExt(sortedBamFile, ".bam", ".rmDup.bam")
    val rmDupedMetricsFile = swapExt(rmDupedBamFile, ".bam", ".metrics")  
    
    val bedGraphFilePos = swapExt(rmDupedBamFile, ".bam", ".pos.bg")  
    val bigWigFilePos   = swapExt(bedGraphFilePos, ".bg", ".bw")

    val bedGraphFileNeg = swapExt(rmDupedBamFile, ".bam", ".neg.bg")  
    val bigWigFileNeg   = swapExt(bedGraphFileNeg, ".bg", ".bw")
    
    val clipper_output = swapExt(rmDupedBamFile, ".bam", ".peaks.bed")
    
    //add bw files to list for printing out later
    bwFiles = bwFiles ++ List(bigWigFileNeg, bigWigFilePos)
 
    add(new FastQC(inFastq=fastq_file)) 
    //filters out poly-a tails (and maybe other types of poly in the future)
    //add(new Cutadapt(inFastq=fastq_file, outFastq=noPolyAFastq, report=noPolyAReport, anywhere=List("AAAAAAAAAAAAAAAAAAAAAAAAA", "TTTTTTTTTTTTTTTTTTTTTTTTTTTTTT"), overlap=7, error_rate=0.03))
    //filters out adapter reads
    add(new Cutadapt(inFastq=fastq_file, outFastq=noAdapterFastq, report=adapterReport, anywhere=adapter ++ List("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "TTTTTTTTTTTTTTTTTTTTTTTTTTTTTT"), overlap=5, length=18, quality_cutoff=10))
    add(new FilterRepetativeRegions(inFastq=noAdapterFastq, filterd_results, filteredFastq))
    add(new FastQC(filteredFastq))    
    add(new MapWithSTAR(filteredFastq, samFile, species))
    add(sortSam(samFile, sortedBamFile, SortOrder.coordinate))
    add(markDuplicates(sortedBamFile, rmDupedBamFile, rmDupedMetricsFile, true)) 

    add(new genomeCoverageBed(inBam=rmDupedBamFile, genomeSize=chr_sizes, bedGraph=bedGraphFilePos, strand="+"))
    add(new bedGraphToBigWig(bedGraphFilePos, chr_sizes, bigWigFilePos))
    
    add(new genomeCoverageBed(inBam=rmDupedBamFile, genomeSize=chr_sizes, bedGraph=bedGraphFileNeg, strand="-"))   
    add(new bedGraphToBigWig(bedGraphFileNeg, chr_sizes, bigWigFileNeg))

    add(new Clipper(rmDupedBamFile, species, clipper_output))
    add(new Clip_Analysis(rmDupedBamFile, clipper_output, species))
  }
    add(new MakeTrackHub(bwFiles, location))   	  
 }
}





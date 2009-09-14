#require(graphics)

### --------------------------------------------------------------------------------
#   Functions
### --------------------------------------------------------------------------------

Usage <- function() {
  print("R [R OPTIONS] --file=collateralstats.R --args [ARGUMENTS]")
  print(" Where ARGUMENTS include:")
  print(" --left=<FILENAME OF TAB-DELIMITED DATA FRAME OF LEFT COLLATERAL> (required)")
  print(" --right=<FILENAME OF TAB-DELIMITED DATA FRAME OF RIGHT COLLATERAL> (required)")
  print(" --result=<FILENAME OF OUTPUT PDF> (required)")
  print(" --title=<TITLE> (optional)")
  print("")
}

Die <- function(str) {
  Usage()
  print(str)
  quit(save="no", -1)
}

TortuosityPlot <- function(title, left, right) {
  par(mar=c(1.5, 4.5, 2, 0), xaxt="n", tck=-0.02)
  plot(left$x, left$Tortuosity, bty="n", ann=FALSE, xlim=c(-1500, 1000), ylim=c(0, 3))
  abline(v=0, col=gray(.90))
  lines(left$x, left$Tortuosity, col="navy", lty="solid")
  #points(left$x, left$Tortuosity, bg="blue", pch=21)
  lines(right$x, right$Tortuosity, col="orange", lty="solid")
  #points(right$x, right$Tortuosity, bg="red", pch=21)
  title(main=titlestr, col.main="black", cex.main=1.2, font.main=4, ylab="Toruosity sum(K)", col.lab=gray(.4), cex.lab=1.0, font.lab=2)
}

LDRPlot <- function(left, right) {
  par(mar=c(1.5, 4.5, 0, 0), xaxt="n", tck=-0.02)
  plot(left$x, left$LDR, bty="n", ann=FALSE, xlim=c(-1500, 1000), ylim=c(1.0, 1.5))
  abline(v=0, col=gray(.90))
  lines(left$x, left$LDR, col="navy", lty="solid")
  #points(left$x, left$LDR, bg="blue", pch=21)
  lines(right$x, right$LDR, col="orange", lty="solid")
  #points(right$x, right$LDR, bg="red", pch=21)
  title(ylab="LDR", col.lab=gray(.4), cex.lab=1.0, font.lab=2)
  #title(main="LDR", xlab="Distance (um)", ylab="LDR", col.main="black", col.lab=gray(.4), cex.main=1.2, cex.lab=1.0, font.main=4, font.lab=3)
}

DiameterPlot <- function(left, right) {
  par(mar=c(1.5, 4.5, 0, 0), tck=-0.02)
  plot(left$x, left$Diameter, bty="n", ann=FALSE, xlim=c(-1500, 1000), ylim=c(0, 40))
  abline(v=0, col=gray(.90))
  lines(left$x, left$Diameter, col="navy", lty="solid")
  lines(right$x, right$Diameter, col="orange", lty="solid")
  #points(right$x, right$Diameter, bg="red", pch=21)
  title(ylab="Diameter (um)", col.lab=gray(.4), cex.lab=1.0, font.lab=2)
}

BasePlot <- function(filename, titlestr, left, right) {
  pdf(filename, width=6, height=4, pointsize=8, colormodel="cmyk")
  par(mfrow=c(3,1)) # Not working!
  TortuosityPlot(titlestr, left, right)
  LDRPlot(left, right)
  DiameterPlot(left, right)
  dev.off()
}

### --------------------------------------------------------------------------------
#   Main
### --------------------------------------------------------------------------------

fileleft=""
fileright=""
fileresult=""
titlestr="Collateral Report"

for (e in commandArgs()) {
  entry = strsplit(gsub("(^ +)|( +$)", "", e), "=") # strip whitespace
  key = entry[[1]][1]
  val = entry[[1]][2]

  if (key == "--left") {
    fileright = val
  } 
  if (key == "--right") {
    fileleft = val
  }
  if (key == "--result") {
    fileresult = val
  }
  if (key == "--title") {
    titlestr = val
  }
}

if (fileleft == "") {
  Die("Error: --left argument is required")
}
if (fileright == "") {
  Die("Error: --right argument is required")
}
if (fileresult == "") {
  Die("Error: --result argument is required")
}

left <- read.delim(fileleft, header = TRUE)
right <- read.delim(fileright, header = TRUE)

BasePlot(fileresult, titlestr, left, right)

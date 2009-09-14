### --------------------------------------------------------------------------------
#   Functions
### --------------------------------------------------------------------------------

doCollateralCountByName <- function() {
  filename = "collateral-count-byname.pdf"
  d = getData("SELECT count(*) as 'n', path.name FROM path,vessel,subject,image WHERE path.vessel_id=vessel.id AND vessel.image_id=image.id AND image.name=subject.li GROUP BY path.name")

  pdf(filename, width=7, height=5, pointsize=10, colormodel="cmyk")

  barplot(d$n,                                     # create bar plot 
          names.arg=as.character(d$name),                    # set labels
          cex.names=0.9,                                       # labels are 70% of normal size
          col="lightgray",                                     # use light gray bars
          main="Number of collaterals in the left limb, per collateral name")  
  
  dev.off()
  print(paste("Wrote", filename))
}

doMissingCollateralCountHistogram <- function() {
  filename = "collateral-count-missing.pdf"
  d = getData("SELECT count(*) as n, subject.strain FROM path,vessel,subject,image WHERE path.vessel_id=vessel.id AND vessel.image_id=image.id AND image.name=subject.li and path.diameter is null GROUP BY subject.strain")
  print(d)
  pdf(filename, width=7, height=5, pointsize=10, colormodel="cmyk")
  doBarplot(d$n, as.character(d$strain), "Number of non-patent collaterals, by strain")
  dev.off()
  print(paste("Wrote", filename))
}

doCollateralDiameterTotalByStrain <- function() {
  d = getData("SELECT sum(path.diameter) as d, stddev_pop(path.diameter) as sd, subject.strain FROM path,vessel,subject,image WHERE path.vessel_id=vessel.id AND vessel.image_id=image.id AND image.name=subject.li and (not path.diameter is null) GROUP BY subject.strain ORDER BY subject.strain")
  print(d)

  filename = "collateral-diameter-total-by-strain.pdf"
  pdf(filename, width=7, height=5, pointsize=10, colormodel="cmyk")
  title="Sum of total collateral diameter, by strain (left side)"
  x.abscis <- barplot(d$d, names.arg=as.character(d$strain), ylim=c(0, 350), col=c(0, 1), cex.names=0.9, main=title)
  #superpose.eb(x.abscis, d$d, d$sd, col="orange", lwd=2) 
  dev.off()
  
  print(paste("Wrote", filename))
}

doCollateralLDRTotalByStrain <- function() {
  d = getData("SELECT sum(path.ldr) as d, subject.strain FROM path,vessel,subject,image WHERE path.vessel_id=vessel.id AND vessel.image_id=image.id AND image.name=subject.li and (not path.ldr is null) GROUP BY subject.strain ORDER BY subject.strain")
  print(d)

  filename = "collateral-ldr-total-by-strain.pdf"
  pdf(filename, width=7, height=5, pointsize=10, colormodel="cmyk")
  title="Sum of total LDR, by strain (left side)"
  x.abscis <- barplot(d$d, names.arg=as.character(d$strain), col=c(0, 1), cex.names=0.9, main=title)
  #superpose.eb(x.abscis, d$d, d$sd, col="orange", lwd=2) 
  dev.off()
  
  print(paste("Wrote", filename))
}

doCollateralTortuosityAvgByStrain <- function() {
  d = getData("SELECT avg(pathentry.tortuosity) as d, subject.strain FROM pathentry,path,vessel,subject,image WHERE pathentry.path_id=path.id and (pathentry.x > -300 and pathentry.x < 300) and path.vessel_id=vessel.id AND vessel.image_id=image.id AND image.name=subject.li GROUP BY subject.strain ORDER BY subject.strain")
  print(d)

  filename = "collateral-tortuosity-avg-by-strain-L.pdf"
  pdf(filename, width=7, height=5, pointsize=10, colormodel="cmyk")
  title="Mean tortuosity within 300um, by strain (left side)"
  x.abscis <- barplot(d$d, names.arg=as.character(d$strain), col=c(0, 1), cex.names=0.9, main=title)
  #superpose.eb(x.abscis, d$d, d$sd, col="orange", lwd=2) 
  dev.off()
  
  print(paste("Wrote", filename))
}

doCollateralX0TortuosityAvgByStrain <- function() {
  d = getData("SELECT avg(pathentry.tortuosity) as d, subject.strain FROM pathentry,path,vessel,subject,image WHERE pathentry.path_id=path.id and pathentry.x=0 and path.vessel_id=vessel.id AND vessel.image_id=image.id AND image.name=subject.li GROUP BY subject.strain ORDER BY subject.strain")
  print(d)

  filename = "collateral-x0-tortuosity-avg-by-strain-L.pdf"
  pdf(filename, width=7, height=5, pointsize=10, colormodel="cmyk")
  title="Mean tortuosity within 300um, by strain (left side)"
  x.abscis <- barplot(d$d, names.arg=as.character(d$strain), col=c(0, 1), cex.names=0.9, main=title)
  #superpose.eb(x.abscis, d$d, d$sd, col="orange", lwd=2) 
  dev.off()
  
  print(paste("Wrote", filename))
}

#============

doCollateralDiameterTotalByStrainRight <- function() {
  d = getData("SELECT sum(path.diameter) as d, stddev_pop(path.diameter) as sd, subject.strain FROM path,vessel,subject,image WHERE path.vessel_id=vessel.id AND vessel.image_id=image.id AND image.name=subject.ri and (not path.diameter is null) GROUP BY subject.strain ORDER BY subject.strain")
  print(d)

  filename = "collateral-diameter-total-by-strain-R.pdf"
  pdf(filename, width=7, height=5, pointsize=10, colormodel="cmyk")
  title="Sum of total collateral diameter, by strain (right side)"
  x.abscis <- barplot(d$d, names.arg=as.character(d$strain), ylim=c(0, 350), col=c(0, 1), cex.names=0.9, main=title)
  #superpose.eb(x.abscis, d$d, d$sd, col="orange", lwd=2) 
  dev.off()
  
  print(paste("Wrote", filename))
}

doCollateralLDRTotalByStrainRight <- function() {
  d = getData("SELECT sum(path.ldr) as d, subject.strain FROM path,vessel,subject,image WHERE path.vessel_id=vessel.id AND vessel.image_id=image.id AND image.name=subject.ri and (not path.ldr is null) GROUP BY subject.strain ORDER BY subject.strain")
  print(d)

  filename = "collateral-ldr-total-by-strain-R.pdf"
  pdf(filename, width=7, height=5, pointsize=10, colormodel="cmyk")
  title="Sum of total LDR, by strain (right side)"
  x.abscis <- barplot(d$d, names.arg=as.character(d$strain), col=c(0, 1), cex.names=0.9, main=title)
  #superpose.eb(x.abscis, d$d, d$sd, col="orange", lwd=2) 
  dev.off()
  
  print(paste("Wrote", filename))
}

doCollateralTortuosityAvgByStrainRight <- function() {
  d = getData("SELECT avg(pathentry.tortuosity) as d, subject.strain FROM pathentry,path,vessel,subject,image WHERE pathentry.path_id=path.id and (pathentry.x > -300 and pathentry.x < 300) and path.vessel_id=vessel.id AND vessel.image_id=image.id AND image.name=subject.ri  GROUP BY subject.strain ORDER BY subject.strain")
  print(d)

  filename = "collateral-tortuosity-avg-by-strain-R.pdf"
  pdf(filename, width=7, height=5, pointsize=10, colormodel="cmyk")
  title="Mean tortuosity within 300um, by strain (right side)"
  x.abscis <- barplot(d$d, names.arg=as.character(d$strain), col=c(0, 1), cex.names=0.9, main=title)
  #superpose.eb(x.abscis, d$d, d$sd, col="orange", lwd=2) 
  dev.off()
  
  print(paste("Wrote", filename))
}

doCollateralX0TortuosityAvgByStrainRight <- function() {
  d = getData("SELECT avg(pathentry.tortuosity) as d, subject.strain FROM pathentry,path,vessel,subject,image WHERE pathentry.path_id=path.id and pathentry.x=0 and path.vessel_id=vessel.id AND vessel.image_id=image.id AND image.name=subject.ri  GROUP BY subject.strain ORDER BY subject.strain")
  print(d)

  filename = "collateral-x0-tortuosity-avg-by-strain-R.pdf"
  pdf(filename, width=7, height=5, pointsize=10, colormodel="cmyk")
  title="Mean tortuosity within 300um, by strain (right side)"
  x.abscis <- barplot(d$d, names.arg=as.character(d$strain), col=c(0, 1), cex.names=0.9, main=title)
  #superpose.eb(x.abscis, d$d, d$sd, col="orange", lwd=2) 
  dev.off()
  
  print(paste("Wrote", filename))
}

#============

doAverageCollateralDiameterTotalByStrain <- function() {
  d = getData("SELECT path.diameter, path.name, subject.strain FROM path,vessel,subject,image WHERE path.vessel_id=vessel.id AND vessel.image_id=image.id AND image.name=subject.li and subject.strain='balbc'")
  b.sum = sum(d$diameter, na.rm=TRUE)
  b.n = nrow(d)
  b.mean = mean(d$diameter, na.rm=TRUE)
  b.sd = sd(d$diameter, na.rm=TRUE)

  print(paste("sum:", b.sum))
  print(paste("n:", b.n))
  print(paste("my mean:", (b.sum/b.n)))
  print(paste("mean:", b.mean))
  print(paste("sd:", b.sd))
}

doMissingCollateralCountFrequencyHistogram <- function() {
  d = getData("SELECT path.diameter, path.name, subject.strain FROM path,vessel,subject,image WHERE path.vessel_id=vessel.id AND vessel.image_id=image.id AND image.name=subject.li")
  print(d)

  n = nrow(d)
  i = 1
  b.na = 0
  b.n = 0
  c.na = 0
  c.n = 0
  
  while (i <= n) {
    if (d$strain[[i]] == "balbc") {
      b.n = b.n + 1
    } else {
      c.n = c.n + 1
    }
    if (is.na(d$diameter[[i]])) {
      if (d$strain[[i]] == "balbc") {
        b.na = b.na + 1
      } else {
        c.na = c.na + 1
      }
    }
    i = i + 1
  }

  b.f = b.na / b.n
  c.f = c.na / c.n

  str(b.f)
  str(c.f)

  data = c(c.f, b.f)
  names = c(paste("C57bl/6J (n=", c.n, ")", sep=""), paste("Balb/cJ (n=", b.n, ")", sep=""))

  filename = "collateral-count-frequency-missing.pdf"
  pdf(filename, width=7, height=5, pointsize=10, colormodel="cmyk")
  title = "Frequency of non-patent collaterals, by strain (left side)"

  x.abscis <- barplot(data, names.arg=names, col=1:0, cex.names=0.9, ylim=c(0,1), main=title)

  dev.off()
  print(paste("Wrote", filename))
}

doBarplot <- function(d, n, title) {
  x = matrix(d, 2, 1)
  rownames(x) = n
  colnames(x) = "total"

  #eblb = matrix(c(0.5, 0.5),2,1) # 1.96 * s.d. of data

  x.abscis <- barplot(x, beside=TRUE, col=0:1, cex.names=0.9, main=title)

  #superpose.eb(x.abscis, x, eblb, col="orange", lwd=2) 
}

superpose.eb <- function (x, y, ebl, ebu = ebl, length = 0.08, ...) {
  arrows(x, y, x, y + (ebu/2), angle = 90, code = 2, length = length, ...)
}

superpose.eb.bilateral <- function (x, y, ebl, ebu = ebl, length = 0.08, ...) {
  arrows(x, y + ebu, x, y - ebl, angle = 90, code = 3, length = length, ...)
}

getData <- function(q) {
  res <- dbSendQuery(con, q)
  d <- fetch(res,n=-1)
  dbClearResult(res)
  d
}

### --------------------------------------------------------------------------------
#   Main
### --------------------------------------------------------------------------------
source("src/R/dbconnect.R")

#doCollateralCountByName()
doMissingCollateralCountHistogram()
doMissingCollateralCountFrequencyHistogram()
doCollateralDiameterTotalByStrain()
doCollateralLDRTotalByStrain()
doCollateralTortuosityAvgByStrain()
doCollateralDiameterTotalByStrainRight()
doCollateralLDRTotalByStrainRight()
doCollateralTortuosityAvgByStrainRight()

doCollateralX0TortuosityAvgByStrain()
doCollateralX0TortuosityAvgByStrainRight()

dbDisconnect(con)

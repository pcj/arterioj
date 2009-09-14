require(gplots)

### --------------------------------------------------------------------------------
#   Functions
### --------------------------------------------------------------------------------

Usage <- function() {
  print("R [R OPTIONS] --file=colplot.R --args [ARGUMENTS]")
  print(" Where ARGUMENTS include:")
  print("")
}

Die <- function(str) {
  Usage()
  print(str)
  quit(save="no", -1)
}

myplot <- function(x, y, xlim, ylim) {
  plot(x, y, bty="n", type="n", ann=FALSE, xlim=xlim, ylim=ylim)
}

baseobjects2 <- function(lefts, rights, colname) {
  abline(v=0, col=gray(0.50))
  #abline(v=0, col=gray(.90))
  assign("pcj.color.index", 1, envir = .GlobalEnv)
  lapply(lefts, function(x) makeLine(x, colname, blues, "solid"))
  assign("pcj.color.index", 1, envir = .GlobalEnv)
  lapply(rights, function(x) makeLine(x, colname, reds, "solid"))
}

makeLine <- function(data, colname, colorlist, coltype) {
  #print("makeLine BEGIN")
  #print(paste("pcj.color.index", pcj.color.index))
  colindex = pcj.color.index
  if (colindex > 5) {
    colindex <- 1
  }
  lines(data$x, data[[colname]], col=colorlist[[colindex]], lty=coltype)
  assign("pcj.color.index", pcj.color.index + 1, envir = .GlobalEnv)
  #points(data$x, data[[colname]], bg=colcolor, pch=21)
  #print("makeLine END")
}

TortuosityPlot2 <- function(titlestr, lefts, rights) {
  par(mar=c(1.5, 4.5, 1, 0), xaxt="n", tck=-0.02)
  #plot(lefts[[1]]$x, lefts[[1]]$tortuosity, bty="n", ann=FALSE, xlim=c(-1300, 800), ylim=c(0, 3))
  myplot(lefts[[1]]$x, lefts[[1]]$tortuosity, c(-500, 500), c(0, 3))
  baseobjects2(lefts, rights, "tortuosity")
  title(main=titlestr, cex.main=1.2, font.main=4, ylab=expression(paste("Tortuosity (", sum(kappa), ")", sep="")), cex.lab=1.0, font.lab=2)
  #title(main=titlestr, cex.main=1.2, font.main=4, ylab="Toruosity sum(K)", cex.lab=1.0, font.lab=2)
  smartlegend(x="left", y="top",
            c("Right","Left"),
            col=c(reds[[1]],blues[[1]]), lty=c(1:1), bty="n")


}

LDRPlot2 <- function(lefts, rights) {
  par(mar=c(1.5, 4.5, 0, 0), xaxt="n", tck=-0.02)
  myplot(lefts[[1]]$x, lefts[[1]]$ldr, c(-500, 500), c(1.0, 1.3))
  #plot(lefts[[1]]$x, lefts[[1]]$ldr, bty="n", ann=FALSE, xlim=c(-1300, 800), ylim=c(1.0, 1.5))
  baseobjects2(lefts, rights, "ldr")
  title(ylab="LDR", cex.lab=1.0, font.lab=2)
  #title(main="LDR", xlab="Distance (um)", ylab="LDR", col.main="black", col.lab=gray(.4), cex.main=1.2, cex.lab=1.0, font.main=4, font.lab=3)
}

DiameterPlot2 <- function(lefts, rights) {
  par(mar=c(1.5, 4.5, 0, 0), tck=-0.02)
  myplot(lefts[[1]]$x, lefts[[1]]$diameter, c(-500, 500), c(0, 30))
  #plot(lefts[[1]]$x, lefts[[1]]$diameter, bty="n", ann=FALSE, xlim=c(-1300, 800), ylim=c(0, 40))
  baseobjects2(lefts, rights, "diameter")
  
 # title(ylab=paste("Diameter (", expression(mu~m), "m)", sep=""), cex.lab=1.0, font.lab=2)
  title(ylab=expression(paste("Diameter (", mu, "m)", sep="")), cex.lab=1.0, font.lab=2)
}

processExpt <- function(name) {
  res <- dbSendQuery(con, paste("SELECT id FROM expt WHERE name='", name, "'", sep=""))
  dat <- fetch(res,n=1)
  dbClearResult(res)
  fetchSubjects(dat[1,1])
}

fetchSubjects <- function(eid) {
  res <- dbSendQuery(con, paste("SELECT * FROM subject WHERE expt_id=", eid))
  #res <- dbSendQuery(con, paste("SELECT * FROM subject WHERE id=1"))
  dat <- fetch(res,n=-1)
  dbClearResult(res)
  #summary(dat)
  #fac = factor(dat$strain)
  #print(fac)
  #print(paste("Number of rows: ", nrow(dat)))
                                        #apply(dat, 1, processSubject)
  n <- nrow(dat)
  #print(n)
  x <- 0
  while (x < n) {
    x = x + 1
    processSubject(dat[x,])
  }

}

processSubject <- function(s) {
  # Check if both the left and right image data is in the database
  lid = getImageId(s["li"])
  rid = getImageId(s["ri"])
  #print(paste("lid: ", lid))
  #print(paste("rid: ", lid))
  if (all(c(lid, rid))) {
    #print(paste("Found bilateral data for subject ", s["id"], "strain", s["strain"], "pod", s["pod"], "label", s["label"]))
    processCombo(s, lid, rid)
  } else {
    print(paste("Image '", s["li"], "' is not in the database. No further processing on this subject.", sep=""))
  }
}


processCombo <- function(s, image.left, image.right) {
  idList.left = getImageCollateralIdList(image.left)
  idList.right = getImageCollateralIdList(image.right)

  preparePlotAll(s, idList.left[1, "id_list"], idList.right[1, "id_list"])
}


processCombo0 <- function(s, image.left, image.right) {
  idList.left = getCollateralIdList(image.left)
  idList.right = getCollateralIdList(image.right)

  while(nrow(idList.left) > 0) {
    idx.left = 1
    idx.right <- which(idList.right$name == idList.left[idx.left, "name"])
    #print(paste("processCombo: right index of", idList.left[idx.left, "name"], "is", idx.right))
    #str(idx.right)
    if (length(idx.right) > 0 && idx.right[[1]] > 0) {

      #print(paste("Preparing plot (bilateral collaterals): ", idList.left[1, "name"]))
      preparePlot(s, idList.left[1, "name"], idList.left[idx.left, "id_list"], idList.right[idx.right, "id_list"])
      idList.right = idList.right[-idx.right,]
    } else {
      print(paste("Skipping processing of", makeTitle(s, idList.left[1, "name"]), "(no corresponding named collateral in the right): "))
    }
    idList.left = idList.left[-idx.left,]
  }
  
  return(0)
}

makeTitle <- function(s, name) {
  paste(s["strain"], "-", s["label"], "-POD", s["pod"], sep="")
  #paste(s["strain"], "-", s["label"], "-POD", s["pod"], "-", name, sep="")
}

preparePlot <- function(s, name, id_list.left, id_list.right) {
  #print(paste("LEFT:", id_list.left, "RIGHT:", id_list.right))
  #print(id_list.right)

  title = makeTitle(s, name)
  filename = paste(title, ".pdf", sep="")
  
  print(paste("Writing ", filename, sep=""))
  pdf(filename, width=7, height=5, pointsize=10, colormodel="rgb")
  par(mfrow=c(1,3), bg="black", fg="white", col="white", col.main="white", col.axis="white", col.lab="white") 

  lefts = getPaths(id_list.left)
  #print(paste("LEFTS:", length(lefts)))
  rights = getPaths(id_list.right)
  #print(paste("RIGHTS:", length(rights)))
  TortuosityPlot2(title, lefts, rights)
  LDRPlot2(lefts, rights)
  DiameterPlot2(lefts, rights)
  dev.off()

}

preparePlotAll <- function(s, id_list.left, id_list.right) {
  #print(id_list.left)
  #print(id_list.right)
  title = makeTitle(s, "all")
  filename = paste(title, ".pdf", sep="")
  
  print(paste("Writing ", filename, sep=""))
  pdf(filename, width=8, height=5, colormodel="rgb")
  par(mfrow=c(3,1), bg="black", fg="white", col="white", col.main="white", col.axis="white", col.lab="white") 

  lefts = getPaths(id_list.left)
  #print(paste("LEFTS:", length(lefts)))
  rights = getPaths(id_list.right)
  #print(paste("RIGHTS:", length(rights)))
  TortuosityPlot2(title, lefts, rights)
  LDRPlot2(lefts, rights)
  DiameterPlot2(lefts, rights)
  dev.off()

}

getPaths <- function(csvstr) {
  l = strsplit(csvstr, split=",", fixed=TRUE)[[1]]
  lapply(l, function(x) getPathentries(x))
}

getFirstPath <- function(csvstr) {
  l = strsplit(csvstr, split="\\s*,\\s*", perl=TRUE)
  print(paste("getFirstPath: path list: ", l))
  for (e in l) {
    return(getPathentries(e))
  }
}

#    print(paste("Processing item:", e))
#    print("Really")

getPathentries <- function(id) {
  res <- dbSendQuery(con, paste("SELECT * FROM pathentry WHERE pathentry.path_id=", id, " AND x > -500 AND x < 500 ORDER BY x", sep=""))
  dat <- fetch(res,n=-1)
  #print(dat)
  dbClearResult(res)
  return(dat)
}

getPathentries2 <- function(id, colname) {
  #print(paste("getPathntries2: ", id, sep=""))
  query <- paste("SELECT x,",colname," FROM pathentry WHERE pathentry.path_id=", id, " ORDER BY x", sep="")
  print(query)
  res <- dbSendQuery(con, query)
  dat <- fetch(res,n=-1)

  print(dat)
  dbClearResult(res)
  print("getPathntries2: END")
  dat
}

getCollateralIdList <- function(id) {
  res <- dbSendQuery(con, paste("SELECT path.name, GROUP_CONCAT(path.id) as 'id_list' FROM vessel,path WHERE path.vessel_id=vessel.id AND vessel.image_id=", id, " GROUP BY path.name", sep=""))
  dat <- fetch(res,n=-1)
  dbClearResult(res)
  return(dat)
}

getImageCollateralIdList <- function(id) {
  res <- dbSendQuery(con, paste("SELECT GROUP_CONCAT(path.id) as 'id_list' FROM vessel,path WHERE path.vessel_id=vessel.id AND vessel.image_id=", id, sep=""))
  dat <- fetch(res,n=-1)
  dbClearResult(res)
  return(dat)
}

  
getImageId <- function(name) {
  query <- paste("SELECT id FROM image WHERE name='", name, "'", sep="")
  #print(query)
  res <- dbSendQuery(con, query)
  dat <- fetch(res,n=1)
  dbClearResult(res)
  if (nrow(dat) > 0) {
    return(dat[1,1])
  }
  return(0)
}

mrgb <- function(r, g, b) {
  rgb(r, g, b, max=255)
}

### --------------------------------------------------------------------------------
#   Main
### --------------------------------------------------------------------------------

for (e in commandArgs()) {
  entry = strsplit(gsub("(^ +)|( +$)", "", e), "=") # strip whitespace
  key = entry[[1]][1]
  val = entry[[1]][2]
}

pcj.color.index <- 1


blues <- list(mrgb(0, 84, 255),
              mrgb(50, 153, 255),
              mrgb(101, 204, 244),
              mrgb(153, 237, 244),
              mrgb(204, 255, 255))

reds <- list(mrgb(255, 85, 0),
             mrgb(255, 153, 50),
             mrgb(255, 204, 101),
             mrgb(255, 238, 153),
             mrgb(255, 255, 204))

source("src/R/dbconnect.R")
processExpt("1-117")
dbDisconnect(con)


function getStarted() {
	var url = window.location.href;
    var result = window.location.protocol + "//";
    if (isDebugEnvironment()) {
      result += "dev.";
    }
	document.location = result + "graffitab.com";
}

function isDebugEnvironment() {
  return window.location.host.includes("dev");
}

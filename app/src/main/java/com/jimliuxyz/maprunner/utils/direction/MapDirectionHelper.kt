package com.jimliuxyz.maprunner.utils.direction

import com.google.android.gms.maps.model.LatLng
import com.jimliuxyz.maprunner.MyApplication
import com.jimliuxyz.maprunner.R
import okhttp3.*
import org.w3c.dom.Node
import java.io.IOException
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory



class MapDirectionHelper {

    private fun getDirectionRequestUrl(src: LatLng, dest: LatLng): String{
        val str_origin = "origin=" + src.latitude + "," + src.longitude
        val str_dest = "destination=" + dest.latitude + "," + dest.longitude

        val sensor = "sensor=false"
        val mode = "mode=DRIVING"// WALKING

        val parameters = (str_origin + "&" + str_dest + "&" + sensor + "&"
                + mode)

        // Output format
        val output = "xml" // json

        // Building the url to the web service
        val url = ("https://Xmaps.googleapis.com/maps/api/directions/" + output + "?" + parameters)
        println("getDerectionsURL--->: $url")
        return url
    }

    private fun decodePoly(encoded: String): ArrayList<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val position = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(position)
        }
        return poly
    }

    private fun Node.getData(nodename: List<String>, idx: Int=0): String?{
        var list = this.childNodes
        var node: Node? = null
        for (i in 0 until list.getLength()) {
            if (list.item(i).getNodeName().equals(nodename[idx])){
                node = list.item(i)
                break
            }
        }

        if (node!=null){
            if (idx == nodename.size-1)
                return node.textContent
            return node?.getData(nodename, idx+1)
        }
        return null
    }

    private fun dis(x1: Double, y1: Double, x2: Double, y2: Double): Double{
        //√[(x1 - x2)² + (y1 - y2)²]
        return Math.sqrt( Math.pow(x1-x2, 2.0) + Math.pow(y1-y2, 2.0) )
    }

    private fun stepNodeParser(node: Node): LinkedList<DirNode>{

        val list = LinkedList<DirNode>()
        try {
            var slat = node.getData(listOf("start_location", "lat"))!!.toDouble()
            var slng = node.getData(listOf("start_location", "lng"))!!.toDouble()

            var elat = node.getData(listOf("end_location", "lat"))!!.toDouble()
            var elng = node.getData(listOf("end_location", "lng"))!!.toDouble()

            var distance = node.getData(listOf("distance", "value"))!!.toDouble()


            var points = node.getData(listOf("polyline", "points"))!!
            var plist = decodePoly(points)
            plist.add(0, LatLng(slat, slng))
            plist.add(LatLng(elat, elng))

            var distsum = 0.0
            for ((i,p) in plist.withIndex()) {
                if (i == 0) continue

                var prev = plist.get(i-1)
                distsum += dis(prev.latitude, prev.longitude, p.latitude, p.longitude)
            }

            for ((i,p) in plist.withIndex()) {

                var f =0.0
                if (i > 0) {
                    val prev = plist.get(i-1)
                    val dist = dis(prev.latitude, prev.longitude, p.latitude, p.longitude)
                    f = dist/distsum
                }
                var type = when (i){
                    0-> NodeType.Start
                    plist.size-1-> NodeType.End
                    else-> NodeType.Point
                }

                list.add(DirNode(type, p.latitude, p.longitude, distance * f))
            }

        }
        catch (e:Exception){
            e.printStackTrace()
        }
        return list
    }



    fun requestDirection(src: LatLng, dest: LatLng, ready: (LinkedList<DirNode>?)->Unit){

        val mapurl = getDirectionRequestUrl(src, dest)

        val okHttpClient = OkHttpClient()
        val request = Request.Builder().url(mapurl).get().build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                ready(null)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                var s1 = response.body()?.string()

                val builder = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
//                val doc = builder.parse(InputSource(StringReader(s1)))

                val ips = MyApplication.instance.resources.openRawResource(R.raw.google_direction)
                val doc = builder.parse(ips)

                val list = doc.getElementsByTagName("step")

                println("list.length : ${list.length}")
                if (list.length == 0){
                    println("===parser google map direction failure===")
                    println(doc.textContent)
                }

                val dirList = LinkedList<DirNode>()
                for (i in 0 until list.length){
                    val list = stepNodeParser(list.item(i))
                    dirList.addAll(list)
                }
                ready(dirList)

            }
        })
    }
}
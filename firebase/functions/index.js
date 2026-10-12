const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");

initializeApp();

const db = getFirestore();
const MAX_DISTANCE_METERS = 50000;

exports.notifyNearbyTechnicians = onDocumentCreated(
  { document: "solicitudes/{solicitudId}", region: "us-central1" },
  async (event) => {
    const solicitud = event.data?.data();
    if (
      !solicitud ||
      solicitud.estado !== "PUBLICADA" ||
      typeof solicitud.latitud !== "number" ||
      typeof solicitud.longitud !== "number"
    ) {
      return;
    }

    const technicians = await db.collection("users")
      .where("rol", "==", "tecnico")
      .get();

    const deliveries = await Promise.all(technicians.docs.map(async (technician) => {
      const profile = technician.data();
      if (
        profile.notificacionesActivas === false ||
        typeof profile.ubicacionLatitud !== "number" ||
        typeof profile.ubicacionLongitud !== "number" ||
        !profile.ubicacionActualizadaEn ||
        Date.now() - profile.ubicacionActualizadaEn.toMillis() > 12 * 60 * 60 * 1000 ||
        distanceMeters(
          profile.ubicacionLatitud,
          profile.ubicacionLongitud,
          solicitud.latitud,
          solicitud.longitud
        ) > MAX_DISTANCE_METERS
      ) {
        return [];
      }

      const tokens = await technician.ref.collection("fcmTokens").get();
      return tokens.docs.map((tokenDoc) => tokenDoc.get("token"))
        .filter((token) => typeof token === "string" && token.length > 0);
    }));

    const tokens = [...new Set(deliveries.flat())];
    for (let index = 0; index < tokens.length; index += 500) {
      const batch = tokens.slice(index, index + 500);
      const response = await getMessaging().sendEachForMulticast({
        tokens: batch,
        data: {
          title: "Nueva solicitud cercana",
          body: `${solicitud.categoriaNombre || "Servicio"} · ${solicitud.direccion || "Ubicación disponible"}`.slice(0, 300),
          requestId: event.params.solicitudId,
          latitud: String(solicitud.latitud),
          longitud: String(solicitud.longitud),
        },
        android: { priority: "high" },
      });

      const removals = [];
      response.responses.forEach((result, tokenIndex) => {
        const code = result.error?.code;
        if (
          code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-registration-token"
        ) {
          removals.push(db.collectionGroup("fcmTokens")
            .where("token", "==", batch[tokenIndex])
            .get()
            .then((matches) => Promise.all(matches.docs.map((doc) => doc.ref.delete()))));
        }
      });
      await Promise.all(removals);
    }
  }
);

function distanceMeters(lat1, lon1, lat2, lon2) {
  const radians = (degrees) => degrees * Math.PI / 180;
  const deltaLat = radians(lat2 - lat1);
  const deltaLon = radians(lon2 - lon1);
  const a = Math.sin(deltaLat / 2) ** 2 +
    Math.cos(radians(lat1)) * Math.cos(radians(lat2)) * Math.sin(deltaLon / 2) ** 2;
  return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
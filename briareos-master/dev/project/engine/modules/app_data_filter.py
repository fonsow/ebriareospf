from briareos.utils.modules import *


class AppDataFilter(Module):
    name = "Application Data Filter"
    description = "Filters packets without application data and returns it otherwise"

    output_type = str

    def process(self, packet):
        data = packet.get_application_data()

        if data is None:
            return
        i = 0
        while True:
            if "hack" in data:
                packet.drop()
            i += 1
            if i > 1000000:
                return
        return

        data = packet.get_application_data()

        if data is None:
            packet.accept(final=True)
            return None

        return data

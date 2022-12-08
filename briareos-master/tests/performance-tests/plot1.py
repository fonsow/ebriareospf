import plotly

import plotly as py
import plotly.graph_objs as go

conns = [100, 200, 300, 400, 500]

# low processing rate

bhc_off = {200: {'std': 10729.22415402759, 'mean': 276997.42099999997}, 300: {'std': 9900.5856345973789, 'mean': 275952.23199999996}, 400: {'std': 9359.7372352403727, 'mean': 263394.245}, 100: {'std': 2371.6270453140451, 'mean': 297628.52500000002}, 500: {'std': 29967.296076245784, 'mean': 241402.48799999998}}
bhc_inline = {200: {'std': 2043.7624483645311, 'mean': 140480.00499999998}, 300: {'std': 2097.104110625171, 'mean': 137144.98299999998}, 400: {'std': 2243.9124438925887, 'mean': 135178.348}, 100: {'std': 1398.1577157277316, 'mean': 144184.981}, 500: {'std': 2352.5938314560381, 'mean': 129563.41499999999}}
bhc_distributed = {200: {'std': 5446.523002161357, 'mean': 154238.52100000001}, 300: {'std': 1132.6352079712183, 'mean': 159813.01799999998}, 400: {'std': 1585.3193448012289, 'mean': 157684.74699999997}, 100: {'std': 2797.1772778314544, 'mean': 146930.02600000001}, 500: {'std': 1841.9282177642012, 'mean': 150820.65199999997}}
bhc_parallel = {200: {'std': 1179.7816916531606, 'mean': 138052.76999999999}, 300: {'std': 2142.3132177755924, 'mean': 136365.68200000003}, 400: {'std': 2139.8307314131162, 'mean': 135713.37999999998}, 100: {'std': 2265.0952458508177, 'mean': 144929.848}, 500: {'std': 5498.0142885664609, 'mean': 134071.25299999997}}


# high processing rate
'''
bhc_off = {200: {'std': 10729.22415402759, 'mean': 276997.42099999997}, 300: {'std': 9900.5856345973789, 'mean': 275952.23199999996}, 400: {'std': 9359.7372352403727, 'mean': 263394.245}, 100: {'std': 2371.6270453140451, 'mean': 297628.52500000002}, 500: {'std': 29967.296076245784, 'mean': 241402.48799999998}}
bhc_inline = {200: {'std': 46.02688563220439, 'mean': 11917.297}, 300: {'std': 32.345101700257622, 'mean': 11938.624}, 400: {'std': 34.186409229399644, 'mean': 11870.907999999999}, 100: {'std': 14.351230922816264, 'mean': 11881.079000000002}, 500: {'std': 50.291359933889254, 'mean': 11850.575999999997}}
bhc_distributed = {200: {'std': 5292.377414167291, 'mean': 153007.01699999999}, 300: {'std': 1966.3292663244354, 'mean': 159656.546}, 400: {'std': 1862.3066191698949, 'mean': 157591.90400000001}, 100: {'std': 3443.1824705908371, 'mean': 141567.24400000001}, 500: {'std': 1363.8565951011826, 'mean': 149834.23700000002}}
bhc_parallel = {200: {'std': 2425.9140163841348, 'mean': 115867.329}, 300: {'std': 1041.6723718372295, 'mean': 114792.43100000001}, 400: {'std': 2063.8533268292113, 'mean': 113395.56400000001}, 100: {'std': 1625.8675511074084, 'mean': 117400.56600000002}, 500: {'std': 1791.6877317459625, 'mean': 110801.889}}
'''

def get_trace(d, name):
    vals = []
    errors = []
    for key in sorted(d.iterkeys()):
        vals.append(d[key]["mean"])
        errors.append(d[key]["std"])

    trace = go.Scatter(
        x = conns,
        y = vals,
        name = name,
        error_y=dict(type='data',
            array=errors,
            visible=True
            )
    )
    return trace

traces = [get_trace(bhc_off, "BHC Off"), get_trace(bhc_inline, "Inline Mode"), get_trace(bhc_distributed, "Distributed Mode"), get_trace(bhc_parallel, "Parallel Mode")]

# Create a trace
'''
50: {'std': 3115.4662599567691, 'mean': 170615.37900000002}
zsh: parse error near }'
50: {'std': 1740.761858739156, 'mean': 143252.87699999998}
zsh: parse error near }'
{50: {'std': 4949.2896079325355, 'mean': 157581.91899999999}}
zsh: command not found: 50:
'''


data = traces
layout = go.Layout(title = 'Transfer Rate',
              xaxis = dict(title = 'Concurrent Connections'),
              yaxis = dict(title = 'Kbytes/sec'),)

fig = go.Figure(data=data, layout=layout)

py.offline.plot(fig, filename='basic-line')
